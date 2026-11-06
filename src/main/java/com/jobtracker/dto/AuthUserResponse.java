package com.jobtracker.dto;


public record AuthUserResponse(
        Long id,
        String name,
        String email,
        String role
) {}
