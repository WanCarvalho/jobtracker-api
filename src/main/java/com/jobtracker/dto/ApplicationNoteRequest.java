package com.jobtracker.dto;

import jakarta.validation.constraints.NotBlank;

public record ApplicationNoteRequest(
        @NotBlank String content
) {}
