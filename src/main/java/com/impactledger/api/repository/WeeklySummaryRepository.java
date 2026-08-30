package com.impactledger.api.repository;

import com.impactledger.api.entity.WeeklySummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklySummaryRepository extends JpaRepository<WeeklySummary, Long> {
    List<WeeklySummary> findByCompanyIdOrderByWeekStartDateDesc(Long companyId);
    Optional<WeeklySummary> findByCompanyIdAndWeekStartDate(Long companyId, LocalDate weekStartDate);
}
