package com.impactledger.api.controller;

import com.impactledger.api.dto.RecognitionRequest;
import com.impactledger.api.dto.RecognitionResponse;
import com.impactledger.api.service.RecognitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recognitions")
@RequiredArgsConstructor
public class RecognitionController {

    private final RecognitionService recognitionService;

    @GetMapping
    public List<RecognitionResponse> getAll() {
        return recognitionService.getAll();
    }

    @PostMapping
    public ResponseEntity<RecognitionResponse> create(@Valid @RequestBody RecognitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recognitionService.create(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        recognitionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
