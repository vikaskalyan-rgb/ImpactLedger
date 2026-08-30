package com.impactledger.api.repository;

import com.impactledger.api.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    List<Task> findByIdIn(List<Long> ids);

    // Trash listing — deliberately separate from TaskSpecifications, which always
    // excludes deleted rows from normal search results.
    List<Task> findByDeletedAtIsNotNullAndCompanyIdOrderByDeletedAtDesc(Long companyId);
    List<Task> findByDeletedAtIsNotNullOrderByDeletedAtDesc();
}