package com.impactledger.api.service;

import com.impactledger.api.dto.WeeklySummaryRequest;
import com.impactledger.api.dto.WeeklySummaryResponse;
import com.impactledger.api.entity.WeeklySummary;
import com.impactledger.api.exception.ResourceNotFoundException;
import com.impactledger.api.repository.WeeklySummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WeeklySummaryService {

    private final WeeklySummaryRepository weeklySummaryRepository;

    @Transactional(readOnly = true)
    public List<WeeklySummaryResponse> list(Long companyId) {
        return weeklySummaryRepository.findByCompanyIdOrderByWeekStartDateDesc(companyId)
                .stream().map(this::toResponse).toList();
    }

    /** One summary per (company, week) — saving again for the same week overwrites it rather than duplicating. */
    @Transactional
    public WeeklySummaryResponse upsert(WeeklySummaryRequest request) {
        WeeklySummary summary = weeklySummaryRepository
                .findByCompanyIdAndWeekStartDate(request.getCompanyId(), request.getWeekStartDate())
                .orElseGet(() -> WeeklySummary.builder()
                        .companyId(request.getCompanyId())
                        .weekStartDate(request.getWeekStartDate())
                        .build());

        summary.setWeekEndDate(request.getWeekEndDate());
        summary.setContent(request.getContent());
        return toResponse(weeklySummaryRepository.save(summary));
    }

    public void delete(Long id) {
        if (!weeklySummaryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Weekly summary not found: " + id);
        }
        weeklySummaryRepository.deleteById(id);
    }

    private WeeklySummaryResponse toResponse(WeeklySummary w) {
        return WeeklySummaryResponse.builder()
                .id(w.getId())
                .companyId(w.getCompanyId())
                .weekStartDate(w.getWeekStartDate())
                .weekEndDate(w.getWeekEndDate())
                .content(w.getContent())
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build();
    }
}
