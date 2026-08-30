package com.impactledger.api.repository;

import com.impactledger.api.entity.Recognition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RecognitionRepository extends JpaRepository<Recognition, Long> {
    List<Recognition> findByCompanyIdAndDateBetweenAndDeletedAtIsNullOrderByDateAsc(Long companyId, LocalDate start, LocalDate end);
    List<Recognition> findByDateBetweenAndDeletedAtIsNullOrderByDateAsc(LocalDate start, LocalDate end);
    List<Recognition> findByDeletedAtIsNull();

    // Trash listing.
    List<Recognition> findByDeletedAtIsNotNullOrderByDeletedAtDesc();
}