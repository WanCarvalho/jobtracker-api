package com.jobtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileImageRequest(@NotBlank @Size(max = 700000) String image) {}
