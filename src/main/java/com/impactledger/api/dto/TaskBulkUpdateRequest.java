package com.impactledger.api.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Only includeInPdf/highlight are supported for now — the two toggles that were
 * previously one-at-a-time-only via the edit dialog. Either field left null is
 * left untouched on every selected task (so callers can flip just one of the two).
 */
@Data
public class TaskBulkUpdateRequest {

    @NotEmpty
    private List<Long> ids;

    private Boolean includeInPdf;
    private Boolean highlight;
}