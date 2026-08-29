package com.impactledger.api.entity;

import com.impactledger.api.entity.enums.Complexity;
import com.impactledger.api.entity.enums.Priority;
import com.impactledger.api.entity.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // ---- Fields you fill manually ----
    @Column(nullable = false)
    private String ticketId;

    @Column(nullable = false)
    private String title;

    // Multi-select: the same ticket can be Design + Infra + Migration + Incident all at once
    @ElementCollection
    @CollectionTable(name = "task_types", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "task_type")
    @BatchSize(size = 50)
    @Builder.Default
    private Set<String> taskTypes = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Complexity complexity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    private LocalDate startDate;
    private LocalDate endDate;

    @ElementCollection
    @CollectionTable(name = "task_pr_links", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "pr_link", length = 1000)
    @BatchSize(size = 50)
    @Builder.Default
    private List<String> prLinks = new ArrayList<>();

    private String designDocLink;

    // ---- Fields typically filled via the AI-assist paste box ----
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String designDecisions;

    @Column(columnDefinition = "TEXT")
    private String impact;

    @ElementCollection
    @CollectionTable(name = "task_tech_stack", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "tech")
    @BatchSize(size = 50)
    @Builder.Default
    private List<String> techStack = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "task_collaborators", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "collaborator")
    @BatchSize(size = 50)
    @Builder.Default
    private List<String> collaborators = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "task_tags", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "tag")
    @BatchSize(size = 50)
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String riskOrBlockerNotes;

    // ---- Meta ----
    @Column(nullable = false)
    @Builder.Default
    private boolean includeInPdf = true;

    // Manually flag your best 5-10 tasks so the PDF's "highlights" section can pick them out
    @Builder.Default
    private boolean highlight = false;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}