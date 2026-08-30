package com.impactledger.api.controller;

import com.impactledger.api.dto.CompanyRequest;
import com.impactledger.api.dto.CompanyResponse;
import com.impactledger.api.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public List<CompanyResponse> getAll() {
        return companyService.getAll();
    }

    @PostMapping
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CompanyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.create(request));
    }

    @PutMapping("/{id}")
    public CompanyResponse update(@PathVariable Long id, @Valid @RequestBody CompanyRequest request) {
        return companyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        companyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trash")
    public List<CompanyResponse> trash() {
        return companyService.getTrash();
    }

    @PostMapping("/{id}/restore")
    public CompanyResponse restore(@PathVariable Long id) {
        return companyService.restore(id);
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> purge(@PathVariable Long id) {
        companyService.purge(id);
        return ResponseEntity.noContent().build();
    }
}