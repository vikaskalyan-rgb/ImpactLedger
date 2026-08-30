package com.impactledger.api.controller;

import com.impactledger.api.dto.TaskBulkUpdateRequest;
import com.impactledger.api.dto.TaskRequest;
import com.impactledger.api.dto.TaskResponse;
import com.impactledger.api.entity.enums.Complexity;
import com.impactledger.api.entity.enums.Priority;
import com.impactledger.api.entity.enums.TaskStatus;
import com.impactledger.api.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public List<TaskResponse> search(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Complexity complexity,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean includeInPdf,
            @RequestParam(required = false) Boolean highlight
    ) {
        return taskService.search(companyId, priority, complexity, status, taskType, startDate, endDate, search, includeInPdf, highlight);
    }

    // Literal "/trash" is matched ahead of the "/{id}" pattern below regardless of
    // declaration order — Spring's path matcher always prefers an exact segment
    // over a variable one, so this never risks binding "trash" as an id.
    @GetMapping("/trash")
    public List<TaskResponse> trash(@RequestParam(required = false) Long companyId) {
        return taskService.getTrash(companyId);
    }

    @GetMapping("/{id}")
    public TaskResponse getById(@PathVariable Long id) {
        return taskService.getById(id);
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request));
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return taskService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public TaskResponse restore(@PathVariable Long id) {
        return taskService.restore(id);
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> purge(@PathVariable Long id) {
        taskService.purge(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/bulk")
    public List<TaskResponse> bulkUpdate(@Valid @RequestBody TaskBulkUpdateRequest request) {
        return taskService.bulkUpdate(request);
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<Void> bulkDelete(@RequestParam List<Long> ids) {
        taskService.bulkDelete(ids);
        return ResponseEntity.noContent().build();
    }
}