package com.impactledger.api.service;

import com.impactledger.api.dto.TaskRequest;
import com.impactledger.api.dto.TaskResponse;
import com.impactledger.api.entity.Company;
import com.impactledger.api.entity.Task;
import com.impactledger.api.entity.enums.Complexity;
import com.impactledger.api.entity.enums.Priority;
import com.impactledger.api.entity.enums.TaskStatus;
import com.impactledger.api.exception.ResourceNotFoundException;
import com.impactledger.api.repository.TaskRepository;
import com.impactledger.api.repository.TaskSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final CompanyService companyService;

    @Transactional(readOnly = true)
    public List<TaskResponse> search(
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
        var spec = TaskSpecifications.filter(
                companyId, priority, complexity, status, taskType,
                startDate, endDate, search, includeInPdf, highlight
        );
        return taskRepository.findAll(spec).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(Long id) {
        return toResponse(getEntity(id));
    }

    public Task getEntity(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Task> getEntitiesByIds(List<Long> ids) {
        List<Task> tasks = taskRepository.findByIdIn(ids);
        if (tasks.size() != new HashSet<>(ids).size()) {
            throw new ResourceNotFoundException("One or more task IDs were not found");
        }
        return tasks;
    }

    public TaskResponse create(TaskRequest request) {
        Company company = companyService.getEntity(request.getCompanyId());
        Task task = Task.builder().build();
        applyRequest(task, request, company);
        return toResponse(taskRepository.save(task));
    }

    public TaskResponse update(Long id, TaskRequest request) {
        Task task = getEntity(id);
        Company company = companyService.getEntity(request.getCompanyId());
        applyRequest(task, request, company);
        return toResponse(taskRepository.save(task));
    }

    public void delete(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task not found: " + id);
        }
        taskRepository.deleteById(id);
    }

    private void applyRequest(Task task, TaskRequest request, Company company) {
        task.setCompany(company);
        task.setTicketId(request.getTicketId());
        task.setTitle(request.getTitle());
        task.setTaskTypes(request.getTaskTypes() != null ? new HashSet<>(request.getTaskTypes()) : new HashSet<>());
        task.setPriority(request.getPriority());
        task.setComplexity(request.getComplexity());
        task.setStatus(request.getStatus());
        task.setStartDate(request.getStartDate());
        task.setEndDate(request.getEndDate());
        task.setPrLinks(request.getPrLinks() != null ? new ArrayList<>(request.getPrLinks()) : new ArrayList<>());
        task.setDesignDocLink(request.getDesignDocLink());
        task.setDescription(request.getDescription());
        task.setDesignDecisions(request.getDesignDecisions());
        task.setImpact(request.getImpact());
        task.setTechStack(request.getTechStack() != null ? new ArrayList<>(request.getTechStack()) : new ArrayList<>());
        task.setCollaborators(request.getCollaborators() != null ? new ArrayList<>(request.getCollaborators()) : new ArrayList<>());
        task.setTags(request.getTags() != null ? new ArrayList<>(request.getTags()) : new ArrayList<>());
        task.setRiskOrBlockerNotes(request.getRiskOrBlockerNotes());
        task.setIncludeInPdf(Objects.requireNonNullElse(request.getIncludeInPdf(), true));
        task.setHighlight(Objects.requireNonNullElse(request.getHighlight(), false));
    }

    private TaskResponse toResponse(Task t) {
        return TaskResponse.builder()
                .id(t.getId())
                .companyId(t.getCompany().getId())
                .companyName(t.getCompany().getName())
                .ticketId(t.getTicketId())
                .title(t.getTitle())
                .taskTypes(t.getTaskTypes())
                .priority(t.getPriority())
                .complexity(t.getComplexity())
                .status(t.getStatus())
                .startDate(t.getStartDate())
                .endDate(t.getEndDate())
                .prLinks(t.getPrLinks())
                .designDocLink(t.getDesignDocLink())
                .description(t.getDescription())
                .designDecisions(t.getDesignDecisions())
                .impact(t.getImpact())
                .techStack(t.getTechStack())
                .collaborators(t.getCollaborators())
                .tags(t.getTags())
                .riskOrBlockerNotes(t.getRiskOrBlockerNotes())
                .includeInPdf(t.isIncludeInPdf())
                .highlight(t.isHighlight())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}