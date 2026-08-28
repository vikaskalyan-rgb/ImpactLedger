package com.impactledger.api.dto;

import com.impactledger.api.entity.enums.Complexity;
import com.impactledger.api.entity.enums.Priority;
import com.impactledger.api.entity.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private Long id;
    private Long companyId;
    private String companyName;

    private String ticketId;
    private String title;
    private Set<String> taskTypes;
    private Priority priority;
    private Complexity complexity;
    private TaskStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> prLinks;
    private String designDocLink;

    private String description;
    private String designDecisions;
    private String impact;
    private List<String> techStack;
    private List<String> collaborators;
    private List<String> tags;
    private String riskOrBlockerNotes;

    private boolean includeInPdf;
    private boolean highlight;

    private Instant createdAt;
    private Instant updatedAt;
}
