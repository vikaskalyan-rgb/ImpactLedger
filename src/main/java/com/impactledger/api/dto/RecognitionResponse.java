package com.impactledger.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionResponse {
    private Long id;
    private Long companyId;
    private LocalDate date;
    private String source;
    private String message;
}
