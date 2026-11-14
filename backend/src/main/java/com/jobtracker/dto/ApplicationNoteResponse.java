package com.jobtracker.dto;

import java.time.LocalDateTime;

public record ApplicationNoteResponse(
        Long id, String content, String author, LocalDateTime createdAt) {}
