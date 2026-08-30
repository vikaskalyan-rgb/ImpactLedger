package com.impactledger.api.service;

import com.impactledger.api.dto.CompanyRequest;
import com.impactledger.api.dto.CompanyResponse;
import com.impactledger.api.entity.Company;
import com.impactledger.api.exception.BadRequestException;
import com.impactledger.api.exception.ResourceNotFoundException;
import com.impactledger.api.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    public List<CompanyResponse> getAll() {
        return companyRepository.findByDeletedAtIsNull().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CompanyResponse create(CompanyRequest request) {
        if (companyRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BadRequestException("A company named '" + request.getName() + "' already exists");
        }
        Company company = Company.builder()
                .name(request.getName().trim())
                .roleTitle(request.getRoleTitle())
                .build();
        return toResponse(companyRepository.save(company));
    }

    @Transactional
    public CompanyResponse update(Long id, CompanyRequest request) {
        Company company = getEntity(id);
        if (!company.getName().equalsIgnoreCase(request.getName())
                && companyRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BadRequestException("A company named '" + request.getName() + "' already exists");
        }
        company.setName(request.getName().trim());
        company.setRoleTitle(request.getRoleTitle());
        return toResponse(companyRepository.save(company));
    }

    // Soft delete — see TaskService.delete for the reasoning. Existing tasks/recognitions
    // keep their (lazy) reference to this company; it just disappears from the switcher.
    @Transactional
    public void delete(Long id) {
        Company company = getEntity(id);
        company.setDeletedAt(Instant.now());
        companyRepository.save(company);
    }

    @Transactional
    public CompanyResponse restore(Long id) {
        Company company = getEntity(id);
        company.setDeletedAt(null);
        return toResponse(companyRepository.save(company));
    }

    @Transactional
    public void purge(Long id) {
        Company company = getEntity(id);
        if (company.getDeletedAt() == null) {
            throw new BadRequestException("Company must be moved to trash before it can be permanently deleted");
        }
        companyRepository.delete(company);
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> getTrash() {
        return companyRepository.findByDeletedAtIsNotNullOrderByDeletedAtDesc().stream().map(this::toResponse).toList();
    }

    public Company getEntity(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + id));
    }

    private CompanyResponse toResponse(Company c) {
        return CompanyResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .roleTitle(c.getRoleTitle())
                .deletedAt(c.getDeletedAt())
                .build();
    }
}