package com.impactledger.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompanyRequest {
    @NotBlank
    private String name;
    private String roleTitle;
}
