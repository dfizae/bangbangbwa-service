package com.backend.bangbangbwa.domain.property.repository;

import com.backend.bangbangbwa.domain.property.entity.PropertyEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertyEnvironmentRepository
        extends JpaRepository<PropertyEnvironment, Long> {

    Optional<PropertyEnvironment> findByPropertyId(Long propertyId);
}
