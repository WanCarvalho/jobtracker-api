package com.jobtracker.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApplicationStageChange(@NotNull Long stageId, @Size(max = 1000) String notes) {}
