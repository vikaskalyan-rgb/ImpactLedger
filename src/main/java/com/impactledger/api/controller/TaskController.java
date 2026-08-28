package com.impactledger.api.controller;

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
}
