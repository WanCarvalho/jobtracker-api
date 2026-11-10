package com.jobtracker.dto;

import java.time.LocalDateTime;

public record ApplicationHistoryResponse(
        Long id,
        Long previousStageId,
        String previousStageName,
        Long newStageId,
        String newStageName,
        String changedBy,
        String notes,
        LocalDateTime changedAt
) {}
