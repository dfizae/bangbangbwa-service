package com.backend.bangbangbwa.domain.document.repository;

import com.backend.bangbangbwa.domain.document.entity.DocumentAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentAnalysisRepository
        extends JpaRepository<DocumentAnalysis, Long> {

    List<DocumentAnalysis> findByDocumentIdIn(List<Long> documentIds);

    void deleteByDocumentId(Long documentId);
}
