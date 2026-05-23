package dev.caixadev.domain.model;

import java.time.LocalDateTime;

public record Project(
        Long id,
        String key,
        String name,
        String description,
        boolean isActive,
        boolean isPublic,
        LocalDateTime createdAt
) {}

