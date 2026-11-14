package com.jobtracker.dto;

import java.time.LocalDateTime;

public record CompanyResponse(
        Long id,
        String name,
        String cnpj,
        String website,
        String description,
        String approvalStatus,
        Long createdById,
        String createdByName,
        Long reviewedById,
        LocalDateTime reviewedAt,
        String reviewNotes) {}
