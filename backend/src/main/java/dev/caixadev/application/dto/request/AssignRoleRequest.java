package dev.caixadev.application.dto.request;

import jakarta.validation.constraints.NotNull;

public record AssignRoleRequest(
        @NotNull Long projectId,
        @NotNull Long roleId
) {}

