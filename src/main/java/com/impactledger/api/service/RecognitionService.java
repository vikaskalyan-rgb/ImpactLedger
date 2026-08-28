package com.impactledger.api.service;

import com.impactledger.api.dto.RecognitionRequest;
import com.impactledger.api.dto.RecognitionResponse;
import com.impactledger.api.entity.Company;
import com.impactledger.api.entity.Recognition;
import com.impactledger.api.exception.ResourceNotFoundException;
import com.impactledger.api.repository.RecognitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecognitionService {

    private final RecognitionRepository recognitionRepository;
    private final CompanyService companyService;

    public List<RecognitionResponse> getAll() {
        return recognitionRepository.findAll().stream().map(this::toResponse).toList();
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

    public void delete(Long id) {
        if (!recognitionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Recognition not found: " + id);
        }
        recognitionRepository.deleteById(id);
    }

    public List<Recognition> findForPeriod(Long companyId, LocalDate start, LocalDate end) {
        if (companyId != null) {
            return recognitionRepository.findByCompanyIdAndDateBetweenOrderByDateAsc(companyId, start, end);
        }
        return recognitionRepository.findByDateBetweenOrderByDateAsc(start, end);
    }

    private RecognitionResponse toResponse(Recognition r) {
        return RecognitionResponse.builder()
                .id(r.getId())
                .companyId(r.getCompany().getId())
                .date(r.getDate())
                .source(r.getSource())
                .message(r.getMessage())
                .build();
    }
}
