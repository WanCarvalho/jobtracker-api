package com.jobtracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRegisterRequest(
        @NotBlank @Size(max=120) String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min=8) String password
) {}
