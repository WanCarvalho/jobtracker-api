package com.jobtracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ApplicationResponse(
        Long id,
        Long companyId,
        String companyName,
        Long stageId,
        String stageName,
        String stageColor,
        boolean finalStage,
        String jobTitle,
        BigDecimal salary,
        String jobUrl,
        String description,
        LocalDate appliedAt
) {}
