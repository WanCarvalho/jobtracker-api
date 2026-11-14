package com.jobtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRequest(
        @NotBlank @Size(max = 180) String name,
        @Size(max = 18) String cnpj,
        @Size(max = 255) String website,
        String description) {}
