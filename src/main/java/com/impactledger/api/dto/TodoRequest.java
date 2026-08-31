package com.impactledger.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TodoRequest {
    @NotNull
    private Long companyId;
    @NotBlank
    private String title;
    private String notes;
    private LocalDate dueDate;
    private boolean completed;
}
