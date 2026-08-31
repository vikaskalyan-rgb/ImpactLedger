package com.impactledger.api.repository;

import com.impactledger.api.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {
    List<Todo> findByCompanyIdAndDeletedAtIsNullOrderByCompletedAscDueDateAscCreatedAtDesc(Long companyId);

    // Trash listing.
    List<Todo> findByCompanyIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(Long companyId);
    List<Todo> findByDeletedAtIsNotNullOrderByDeletedAtDesc();
}
