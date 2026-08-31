package com.impactledger.api.service;

import com.impactledger.api.dto.TodoRequest;
import com.impactledger.api.dto.TodoResponse;
import com.impactledger.api.entity.Todo;
import com.impactledger.api.exception.BadRequestException;
import com.impactledger.api.exception.ResourceNotFoundException;
import com.impactledger.api.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoRepository todoRepository;

    @Transactional(readOnly = true)
    public List<TodoResponse> list(Long companyId) {
        return todoRepository.findByCompanyIdAndDeletedAtIsNullOrderByCompletedAscDueDateAscCreatedAtDesc(companyId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public TodoResponse create(TodoRequest request) {
        Todo todo = Todo.builder()
                .companyId(request.getCompanyId())
                .title(request.getTitle())
                .notes(request.getNotes())
                .dueDate(request.getDueDate())
                .completed(request.isCompleted())
                .build();
        return toResponse(todoRepository.save(todo));
    }

    @Transactional
    public TodoResponse update(Long id, TodoRequest request) {
        Todo todo = getEntity(id);
        todo.setTitle(request.getTitle());
        todo.setNotes(request.getNotes());
        todo.setDueDate(request.getDueDate());
        todo.setCompleted(request.isCompleted());
        return toResponse(todoRepository.save(todo));
    }

    /** Lightweight endpoint for the checkbox toggle — no need to round-trip the full request body for that. */
    @Transactional
    public TodoResponse setCompleted(Long id, boolean completed) {
        Todo todo = getEntity(id);
        todo.setCompleted(completed);
        return toResponse(todoRepository.save(todo));
    }

    public Todo getEntity(Long id) {
        return todoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Todo not found: " + id));
    }

    // Soft delete — see TaskService.delete for the reasoning.
    @Transactional
    public void delete(Long id) {
        Todo todo = getEntity(id);
        todo.setDeletedAt(Instant.now());
        todoRepository.save(todo);
    }

    @Transactional
    public TodoResponse restore(Long id) {
        Todo todo = getEntity(id);
        todo.setDeletedAt(null);
        return toResponse(todoRepository.save(todo));
    }

    @Transactional
    public void purge(Long id) {
        Todo todo = getEntity(id);
        if (todo.getDeletedAt() == null) {
            throw new BadRequestException("Todo must be moved to trash before it can be permanently deleted");
        }
        todoRepository.delete(todo);
    }

    @Transactional(readOnly = true)
    public List<TodoResponse> getTrash(Long companyId) {
        List<Todo> trashed = companyId != null
                ? todoRepository.findByCompanyIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(companyId)
                : todoRepository.findByDeletedAtIsNotNullOrderByDeletedAtDesc();
        return trashed.stream().map(this::toResponse).toList();
    }

    private TodoResponse toResponse(Todo t) {
        return TodoResponse.builder()
                .id(t.getId())
                .companyId(t.getCompanyId())
                .title(t.getTitle())
                .notes(t.getNotes())
                .dueDate(t.getDueDate())
                .completed(t.isCompleted())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .deletedAt(t.getDeletedAt())
                .build();
    }
}
