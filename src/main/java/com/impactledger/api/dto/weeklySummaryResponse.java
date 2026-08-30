package com.impactledger.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklySummaryResponse {
    private Long id;
    private Long companyId;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
}
