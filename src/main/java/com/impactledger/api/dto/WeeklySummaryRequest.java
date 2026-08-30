package com.impactledger.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class WeeklySummaryRequest {
    @NotNull
    private Long companyId;
    @NotNull
    private LocalDate weekStartDate;
    @NotNull
    private LocalDate weekEndDate;
    @NotBlank
    private String content;
}
