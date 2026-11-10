package com.jobtracker.dto;


public record StageResponse(
        Long id,
        String name,
        Integer position,
        String color,
        boolean isFinal,
        boolean isActive
) {}
