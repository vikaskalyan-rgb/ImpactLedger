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
public class TodoResponse {
    private Long id;
    private Long companyId;
    private String title;
    private String notes;
    private LocalDate dueDate;
    private boolean completed;
    private Instant createdAt;
    private Instant updatedAt;

    /** Null unless this to-do is in the trash. */
    private Instant deletedAt;
}
