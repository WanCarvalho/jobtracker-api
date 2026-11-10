package com.jobtracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ApplicationRequest(
        @NotNull Long companyId,
        @NotNull Long stageId,
        @NotBlank @Size(max=180) String jobTitle,
        @DecimalMin("0.0") BigDecimal salary,
        @Size(max=1000) String jobUrl,
        String description,
        @NotNull LocalDate appliedAt
) {}
