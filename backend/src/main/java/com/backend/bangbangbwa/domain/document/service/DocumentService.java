package com.backend.bangbangbwa.domain.document.service;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.domain.document.dto.DocumentResponse;
import com.backend.bangbangbwa.domain.document.dto.DocumentSummaryResponse;
import com.backend.bangbangbwa.domain.document.entity.Document;
import com.backend.bangbangbwa.domain.document.entity.DocumentAnalysis;
import com.backend.bangbangbwa.domain.document.entity.DocumentType;
import com.backend.bangbangbwa.domain.document.event.DocumentUploadedEvent;
import com.backend.bangbangbwa.domain.document.repository.DocumentAnalysisRepository;
import com.backend.bangbangbwa.domain.document.repository.DocumentRepository;
import com.backend.bangbangbwa.domain.document.storage.PropertyDocumentStorage;
import com.backend.bangbangbwa.domain.document.storage.PropertyDocumentStorage.StoredFile;
import com.backend.bangbangbwa.domain.property.entity.Property;
import com.backend.bangbangbwa.domain.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {

    private static final long MAX_DOCUMENT_SIZE = 10L * 1024 * 1024;
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final DocumentRepository documentRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyDocumentStorage documentStorage;
    private final DocumentPdfGenerator documentPdfGenerator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DocumentResponse register(
            Long propertyId,
            Long userId,
            String documentType,
            MultipartFile file
    ) {
        Property property = getOwnedProperty(propertyId, userId);
        DocumentType type = DocumentType.from(documentType);
        validatePdf(file);

        StoredFile stored = documentStorage.upload(propertyId, file);
        try {
            Document document = documentRepository.save(
                    Document.builder()
                            .property(property)
                            .documentType(type)
                            .fileKey(stored.fileKey())
                            .build()
            );
            publishAnalysis(document, property);
            return DocumentResponse.from(document);
        } catch (RuntimeException exception) {
            documentStorage.delete(stored.fileKey());
            throw exception;
        }
    }

    @Transactional
    public DocumentResponse replace(
            Long propertyId,
            Long userId,
            Long documentId,
            MultipartFile file
    ) {
        Property property = getOwnedProperty(propertyId, userId);
        Document document = getDocument(documentId, propertyId);
        validatePdf(file);

        String oldFileKey = document.getFileKey();
        StoredFile stored = documentStorage.upload(propertyId, file);
        try {
            document.reupload(stored.fileKey());
            publishAnalysis(document, property);
        } catch (RuntimeException exception) {
            documentStorage.delete(stored.fileKey());
            throw exception;
        }

        if (!oldFileKey.equals(stored.fileKey())) {
            documentStorage.delete(oldFileKey);
        }
        return DocumentResponse.from(document);
    }

    @Transactional
    public void delete(Long propertyId, Long userId, Long documentId) {
        getOwnedProperty(propertyId, userId);
        Document document = getDocument(documentId, propertyId);

        String fileKey = document.getFileKey();
        documentAnalysisRepository.deleteByDocumentId(documentId);
        documentRepository.delete(document);
        documentStorage.delete(fileKey);
    }

    public List<DocumentSummaryResponse> getDocuments(Long propertyId) {
        validateProperty(propertyId);

        List<Document> documents =
                documentRepository.findByPropertyIdOrderByCreatedAtAsc(propertyId);
        if (documents.isEmpty()) {
            return List.of();
        }

        List<Long> documentIds = documents.stream()
                .map(Document::getId)
                .toList();
        Map<Long, DocumentAnalysis> analysisByDocumentId =
                documentAnalysisRepository.findByDocumentIdIn(documentIds).stream()
                        .collect(Collectors.toMap(
                                analysis -> analysis.getDocument().getId(),
                                analysis -> analysis
                        ));

        return documents.stream()
                .map(document -> DocumentSummaryResponse.from(
                        document,
                        analysisByDocumentId.get(document.getId())
                ))
                .toList();
    }

    /**
     * 매물에 등록된 서류 분석 결과를 한 개의 합본 PDF로 만듭니다.
     * 등기부등본·건축물대장이 모두 분석 완료된 경우에만 제공합니다.
     */
    public DocumentPdfContent download(Long propertyId) {
        validateProperty(propertyId);

        List<Document> documents =
                documentRepository.findByPropertyIdOrderByCreatedAtAsc(propertyId);
        if (documents.isEmpty()) {
            throw new CustomException(ErrorCode.DOCUMENT_NOT_READY);
        }

        Map<DocumentType, Document> documentByType = documents.stream()
                .collect(Collectors.toMap(
                        Document::getDocumentType,
                        document -> document,
                        (older, newer) -> newer
                ));
        Map<Long, DocumentAnalysis> analysisByDocumentId =
                documentAnalysisRepository
                        .findByDocumentIdIn(documents.stream().map(Document::getId).toList())
                        .stream()
                        .collect(Collectors.toMap(
                                analysis -> analysis.getDocument().getId(),
                                analysis -> analysis
                        ));

        // 서류 순서는 DocumentType 선언 순서(등기부등본 → 건축물대장)를 따른다.
        List<DocumentPdfGenerator.DocumentSection> sections = new ArrayList<>();
        for (DocumentType type : DocumentType.values()) {
            Document document = documentByType.get(type);
            if (document == null || !document.isCompleted()) {
                throw new CustomException(ErrorCode.DOCUMENT_NOT_READY);
            }

            DocumentAnalysis analysis = analysisByDocumentId.get(document.getId());
            if (analysis == null) {
                throw new CustomException(ErrorCode.DOCUMENT_NOT_READY);
            }

            sections.add(new DocumentPdfGenerator.DocumentSection(
                    type.getValue(),
                    analysis.getRiskLevel().getValue(),
                    analysis.getSummary(),
                    analysis.getResultJson()
            ));
        }

        byte[] pdf = documentPdfGenerator.generate(propertyId, sections);

        String fileName = "서류분석_" + propertyId + ".pdf";
        return new DocumentPdfContent(fileName, pdf);
    }

    public record DocumentPdfContent(String fileName, byte[] content) {
    }

    private void publishAnalysis(Document document, Property property) {
        eventPublisher.publishEvent(new DocumentUploadedEvent(
                document.getId(),
                document.getDocumentType(),
                resolveAddress(property)
        ));
    }

    private String resolveAddress(Property property) {
        if (StringUtils.hasText(property.getRoadAddress())) {
            return property.getRoadAddress();
        }
        String sigungu = property.getSigungu() != null ? property.getSigungu() : "";
        String dong = property.getDong() != null ? property.getDong() : "";
        return (sigungu + " " + dong).trim();
    }

    private Property getOwnedProperty(Long propertyId, Long userId) {
        Property property = propertyRepository
                .findByIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.PROPERTY_NOT_FOUND)
                );
        if (!property.getRegistrant().getId().equals(userId)) {
            throw new CustomException(ErrorCode.PROPERTY_ACCESS_DENIED);
        }
        return property;
    }

    private void validateProperty(Long propertyId) {
        if (propertyRepository.findByIdAndDeletedAtIsNull(propertyId).isEmpty()) {
            throw new CustomException(ErrorCode.PROPERTY_NOT_FOUND);
        }
    }

    private Document getDocument(Long documentId, Long propertyId) {
        return documentRepository
                .findByIdAndPropertyId(documentId, propertyId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.DOCUMENT_NOT_FOUND)
                );
    }

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }
        if (file.getSize() > MAX_DOCUMENT_SIZE) {
            throw new CustomException(ErrorCode.DOCUMENT_FILE_SIZE_EXCEEDED);
        }
        if (!PDF_CONTENT_TYPE.equals(file.getContentType())) {
            throw new CustomException(ErrorCode.UNSUPPORTED_DOCUMENT_FILE_TYPE);
        }
    }
}
