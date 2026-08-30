package com.impactledger.api.service;

import com.impactledger.api.dto.RecognitionRequest;
import com.impactledger.api.dto.RecognitionResponse;
import com.impactledger.api.entity.Company;
import com.impactledger.api.entity.Recognition;
import com.impactledger.api.exception.BadRequestException;
import com.impactledger.api.exception.ResourceNotFoundException;
import com.impactledger.api.repository.RecognitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecognitionService {

    private final RecognitionRepository recognitionRepository;
    private final CompanyService companyService;

    public List<RecognitionResponse> getAll() {
        return recognitionRepository.findByDeletedAtIsNull().stream().map(this::toResponse).toList();
    }

    public RecognitionResponse create(RecognitionRequest request) {
        Company company = companyService.getEntity(request.getCompanyId());
        Recognition recognition = Recognition.builder()
                .company(company)
                .date(request.getDate())
                .source(request.getSource())
                .message(request.getMessage())
                .build();
        return toResponse(recognitionRepository.save(recognition));
    }

    public Recognition getEntity(Long id) {
        return recognitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recognition not found: " + id));
    }

    // Soft delete — see TaskService.delete for the reasoning.
    @Transactional
    public void delete(Long id) {
        Recognition recognition = getEntity(id);
        recognition.setDeletedAt(Instant.now());
        recognitionRepository.save(recognition);
    }

    @Transactional
    public RecognitionResponse restore(Long id) {
        Recognition recognition = getEntity(id);
        recognition.setDeletedAt(null);
        return toResponse(recognitionRepository.save(recognition));
    }

    @Transactional
    public void purge(Long id) {
        Recognition recognition = getEntity(id);
        if (recognition.getDeletedAt() == null) {
            throw new BadRequestException("Recognition must be moved to trash before it can be permanently deleted");
        }
        recognitionRepository.delete(recognition);
    }

    @Transactional(readOnly = true)
    public List<RecognitionResponse> getTrash() {
        return recognitionRepository.findByDeletedAtIsNotNullOrderByDeletedAtDesc().stream().map(this::toResponse).toList();
    }

    public List<Recognition> findForPeriod(Long companyId, LocalDate start, LocalDate end) {
        if (companyId != null) {
            return recognitionRepository.findByCompanyIdAndDateBetweenAndDeletedAtIsNullOrderByDateAsc(companyId, start, end);
        }
        return recognitionRepository.findByDateBetweenAndDeletedAtIsNullOrderByDateAsc(start, end);
    }

    private RecognitionResponse toResponse(Recognition r) {
        return RecognitionResponse.builder()
                .id(r.getId())
                .companyId(r.getCompany().getId())
                .date(r.getDate())
                .source(r.getSource())
                .message(r.getMessage())
                .deletedAt(r.getDeletedAt())
                .build();
    }
}