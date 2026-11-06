package com.jobtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyReviewRequest(
        @NotBlank @Size(max=1000) String reason
) {}
