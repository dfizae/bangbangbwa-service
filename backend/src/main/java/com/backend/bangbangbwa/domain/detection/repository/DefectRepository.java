package com.backend.bangbangbwa.domain.detection.repository;

import com.backend.bangbangbwa.domain.detection.entity.Defect;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DefectRepository extends JpaRepository<Defect, Long> {

    List<Defect> findByCapture_IdOrderByCreatedAtAsc(
            Long captureId
    );

}
