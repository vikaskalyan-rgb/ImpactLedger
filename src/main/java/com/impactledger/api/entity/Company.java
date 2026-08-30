package com.impactledger.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "companies", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // Optional: role held at this company, used only for your own context (never printed on PDF)
    private String roleTitle;

    private Instant createdAt;

    // Soft delete — see Task.deletedAt for the pattern this follows. A deleted company
    // disappears from the switcher but its tasks/recognitions keep pointing at it, and
    // its name stays reserved (uniqueness check ignores deletedAt) until it's restored
    // or purged, so you can't accidentally create a duplicate while the old one is in trash.
    private Instant deletedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }
}