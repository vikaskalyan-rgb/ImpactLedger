package com.impactledger.api.controller;

import com.impactledger.api.dto.WeeklySummaryRequest;
import com.impactledger.api.dto.WeeklySummaryResponse;
import com.impactledger.api.service.WeeklySummaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/weekly-summaries")
@RequiredArgsConstructor
public class WeeklySummaryController {

    private final WeeklySummaryService weeklySummaryService;

    @GetMapping
    public List<WeeklySummaryResponse> list(@RequestParam Long companyId) {
        return weeklySummaryService.list(companyId);
    }

    @PostMapping
    public WeeklySummaryResponse upsert(@Valid @RequestBody WeeklySummaryRequest request) {
        return weeklySummaryService.upsert(request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        weeklySummaryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
