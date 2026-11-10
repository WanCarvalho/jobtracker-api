package com.jobtracker.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StageRequest(
        @NotBlank @Size(max=100) String name,
        @NotNull @Min(0) Integer position,
        @NotBlank String color,
        boolean isFinal
) {}
