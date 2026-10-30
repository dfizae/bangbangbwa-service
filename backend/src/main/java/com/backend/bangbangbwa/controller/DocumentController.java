package com.backend.bangbangbwa.controller;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.common.response.ApiResponse;
import com.backend.bangbangbwa.domain.document.dto.DocumentResponse;
import com.backend.bangbangbwa.domain.document.dto.DocumentSummaryResponse;
import com.backend.bangbangbwa.domain.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/properties")
@Tag(name = "Property Document", description = "매물 서류(AI 검증) API")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(
            value = "/{propertyId:\\d+}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "매물 서류 등록", description = "중개사가 매물에 서류(PDF)를 등록하고 AI 분석을 시작합니다")
    public ResponseEntity<ApiResponse<DocumentResponse>>
    registerDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long propertyId,
            @RequestParam("documentType") String documentType,
            @RequestPart("file") MultipartFile file
    ) {
        DocumentResponse response = documentService.register(
                propertyId,
                extractUserId(jwt),
                documentType,
                file
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "서류가 등록되었습니다. AI 분석이 진행됩니다.",
                        response
                )
        );
    }

    @PutMapping(
            value = "/{propertyId:\\d+}/documents/{documentId:\\d+}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "매물 서류 교체", description = "기존 서류를 새 PDF로 교체하고 재분석합니다")
    public ResponseEntity<ApiResponse<DocumentResponse>>
    replaceDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long propertyId,
            @PathVariable Long documentId,
            @RequestPart("file") MultipartFile file
    ) {
        DocumentResponse response = documentService.replace(
                propertyId,
                extractUserId(jwt),
                documentId,
                file
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "서류가 교체되었습니다. 재분석이 진행됩니다.",
                        response
                )
        );
    }

    @DeleteMapping("/{propertyId:\\d+}/documents/{documentId:\\d+}")
    @Operation(summary = "매물 서류 삭제", description = "중개사가 매물 서류를 삭제합니다")
    public ResponseEntity<ApiResponse<Void>>
    deleteDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long propertyId,
            @PathVariable Long documentId
    ) {
        documentService.delete(propertyId, extractUserId(jwt), documentId);

        return ResponseEntity.ok(
                ApiResponse.success("서류가 삭제되었습니다.")
        );
    }

    @GetMapping("/{propertyId:\\d+}/documents")
    @Operation(summary = "매물 서류·분석 목록 조회", description = "매물에 등록된 서류와 AI 분석 결과를 조회합니다")
    public ResponseEntity<ApiResponse<List<DocumentSummaryResponse>>>
    getDocuments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long propertyId
    ) {
        extractUserId(jwt);

        List<DocumentSummaryResponse> response =
                documentService.getDocuments(propertyId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "매물 서류 조회가 완료되었습니다.",
                        response
                )
        );
    }

    @GetMapping("/{propertyId:\\d+}/documents/download")
    @Operation(
            summary = "매물 서류 분석 PDF 다운로드",
            description = "등기부등본·건축물대장 분석 결과를 하나의 PDF로 묶어 다운로드합니다. "
                    + "두 서류가 모두 분석 완료된 경우에만 제공됩니다."
    )
    public ResponseEntity<byte[]> downloadDocuments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long propertyId
    ) {
        extractUserId(jwt);

        DocumentService.DocumentPdfContent pdf =
                documentService.download(propertyId);

        ContentDisposition contentDisposition = ContentDisposition
                .attachment()
                .filename(pdf.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.content());
    }

    private Long extractUserId(Jwt jwt) {
        if (jwt == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return Long.valueOf(jwt.getSubject());
    }
}
