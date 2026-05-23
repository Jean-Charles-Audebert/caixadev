package dev.caixadev.application.dto.response;

import dev.caixadev.domain.model.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.email(), user.displayName(), user.createdAt());
    }
}

