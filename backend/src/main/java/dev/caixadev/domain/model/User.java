package dev.caixadev.domain.model;

import java.time.LocalDateTime;

public record User(
        Long id,
        String email,
        String passwordHash,
        String displayName,
        boolean isSuperAdmin,
        LocalDateTime createdAt
) {}

