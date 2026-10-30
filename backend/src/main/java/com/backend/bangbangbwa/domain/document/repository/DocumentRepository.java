package com.backend.bangbangbwa.domain.document.repository;

import com.backend.bangbangbwa.domain.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByPropertyIdOrderByCreatedAtAsc(Long propertyId);

    Optional<Document> findByIdAndPropertyId(Long id, Long propertyId);
}
