package com.jobtracker.dto;

import java.util.List;

public record StageReorderRequest(
        List<Long> stageIds
) {}
