package com.impactledger.api.repository;

import com.impactledger.api.entity.Task;
import com.impactledger.api.entity.enums.Complexity;
import com.impactledger.api.entity.enums.Priority;
import com.impactledger.api.entity.enums.TaskStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a dynamic WHERE clause from whichever filters the caller actually supplied.
 * Every parameter is optional; null/blank values are simply skipped.
 */
public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> filter(
            Long companyId,
            Priority priority,
            Complexity complexity,
            TaskStatus status,
            String taskType,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Boolean includeInPdf,
            Boolean highlight
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (companyId != null) {
                predicates.add(cb.equal(root.get("company").get("id"), companyId));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (complexity != null) {
                predicates.add(cb.equal(root.get("complexity"), complexity));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (taskType != null && !taskType.isBlank()) {
                predicates.add(cb.isMember(taskType, root.get("taskTypes")));
            }
            // Overlap with [startDate, endDate]: task.endDate >= filterStart AND task.startDate <= filterEnd
            if (startDate != null) {
                predicates.add(cb.or(
                        cb.greaterThanOrEqualTo(root.get("endDate"), startDate),
                        cb.and(cb.isNull(root.get("endDate")), cb.greaterThanOrEqualTo(root.get("startDate"), startDate))
                ));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), endDate));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("ticketId")), like),
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like)
                ));
            }
            if (includeInPdf != null) {
                predicates.add(cb.equal(root.get("includeInPdf"), includeInPdf));
            }
            if (highlight != null) {
                predicates.add(cb.equal(root.get("highlight"), highlight));
            }

            if (query != null) {
                query.distinct(true);
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
