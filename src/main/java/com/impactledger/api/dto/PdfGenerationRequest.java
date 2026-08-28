package com.impactledger.api.dto;

import com.impactledger.api.entity.enums.AppraisalType;
import com.impactledger.api.entity.enums.PdfMode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PdfGenerationRequest {

    @NotNull
    private PdfMode mode; // APPRAISAL or MONTHLY

    // required when mode = APPRAISAL
    private AppraisalType appraisalType; // MIDYEAR or YEAR_END

    @NotNull
    private Integer year;

    // required when mode = MONTHLY (1-12)
    private Integer month;

    // Optional custom range override for MONTHLY mode. If absent, derived from year+month.
    private LocalDate customStartDate;
    private LocalDate customEndDate;

    // null = across all companies
    private Long companyId;

    @NotEmpty
    private List<Long> taskIds;

    // Shown on the cover page instead of company/employee-style labels
    private String profileName;
    private String profileTitle;
}
