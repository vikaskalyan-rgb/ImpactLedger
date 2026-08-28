package com.impactledger.api.dto;

import com.impactledger.api.entity.enums.Complexity;
import com.impactledger.api.entity.enums.Priority;
import com.impactledger.api.entity.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
public class TaskRequest {

    @NotNull
    private Long companyId;

    // ---- manual fields ----
    @NotBlank
    private String ticketId;

    @NotBlank
    private String title;

    private Set<String> taskTypes;

    @NotNull
    private Priority priority;

    @NotNull
    private Complexity complexity;

    @NotNull
    private TaskStatus status;

    private LocalDate startDate;
    private LocalDate endDate;

    private List<String> prLinks;
    private String designDocLink;

    // ---- AI-assist fields ----
    private String description;
    private String designDecisions;
    private String impact;
    private List<String> techStack;
    private List<String> collaborators;
    private List<String> tags;
    private String riskOrBlockerNotes;

    // ---- meta ----
    private Boolean includeInPdf;
    private Boolean highlight;
}
