package dev.caixadev.domain.model;

/** Association d'un utilisateur à un projet via un rôle. */
public record UserProjectRole(
        User user,
        Project project,
        Role role
) {}

