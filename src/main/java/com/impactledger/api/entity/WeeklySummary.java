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
 * One entry per (company, week) — a short AI-assisted reflection on that week's
 * work. Deliberately uses a plain companyId column rather than a @ManyToOne
 * Company relation: this app doesn't need the company's name/roleTitle when
 * reading a summary back, and every JPA relation we've added has caused a
 * lazy-loading proxy bug at some point this build — simplest to just not have one here.
 */
@Entity
@Table(name = "weekly_summaries", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "week_start_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private LocalDate weekStartDate;

    @Column(nullable = false)
    private LocalDate weekEndDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

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
