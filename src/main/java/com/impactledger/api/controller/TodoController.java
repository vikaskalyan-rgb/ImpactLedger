package com.impactledger.api.controller;

import com.impactledger.api.dto.TodoRequest;
import com.impactledger.api.dto.TodoResponse;
import com.impactledger.api.service.TodoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    // Literal "/trash" is matched ahead of "/{id}" regardless of declaration
    // order — Spring's path matcher always prefers an exact segment over a
    // variable one — same as the equivalent /api/tasks/trash endpoint.
    @GetMapping("/trash")
    public List<TodoResponse> trash(@RequestParam(required = false) Long companyId) {
        return todoService.getTrash(companyId);
    }

    @GetMapping
    public List<TodoResponse> list(@RequestParam Long companyId) {
        return todoService.list(companyId);
    }

    @PostMapping
    public ResponseEntity<TodoResponse> create(@Valid @RequestBody TodoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(todoService.create(request));
    }

    @PutMapping("/{id}")
    public TodoResponse update(@PathVariable Long id, @Valid @RequestBody TodoRequest request) {
        return todoService.update(id, request);
    }

    @PatchMapping("/{id}/complete")
    public TodoResponse setCompleted(@PathVariable Long id, @RequestParam boolean completed) {
        return todoService.setCompleted(id, completed);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        todoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public TodoResponse restore(@PathVariable Long id) {
        return todoService.restore(id);
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> purge(@PathVariable Long id) {
        todoService.purge(id);
        return ResponseEntity.noContent().build();
    }
}
