package com.impactledger.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A quick checklist item — "ping manager about promo doc", "renew AWS cert" —
 * deliberately lightweight and separate from Task (which is ticket/appraisal
 * evidence with priority, complexity, PR links, etc.). Plain companyId column,
 * not a @ManyToOne relation, for the same reason as WeeklySummary: nothing here
 * ever needs to read the company's name back, and every relation added to this
 * codebase has caused a lazy-loading proxy bug at some point.
 */
@Entity
@Table(name = "todos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDate dueDate;

    @Column(nullable = false)
    private boolean completed;

    private Instant createdAt;
    private Instant updatedAt;

    // Soft delete — see Task.deletedAt for the pattern this follows.
    private Instant deletedAt;

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
