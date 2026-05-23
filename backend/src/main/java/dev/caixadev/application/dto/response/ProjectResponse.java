package dev.caixadev.application.dto.response;

import java.util.List;

public record ProjectResponse(
        Long id,
        String key,
        String name,
        String description,
        boolean isActive,
        boolean isPublic,
        List<String> roles
) {}

