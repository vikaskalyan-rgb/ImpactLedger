package com.impactledger.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RecognitionRequest {
    @NotNull
    private Long companyId;
    @NotNull
    private LocalDate date;
    @NotBlank
    private String source;
    @NotBlank
    private String message;
}
