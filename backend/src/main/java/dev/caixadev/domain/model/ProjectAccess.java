package dev.caixadev.domain.model;

import java.util.List;

/** Un projet avec la liste des rôles attribués à un utilisateur donné. */
public record ProjectAccess(
        Project project,
        List<String> roles
) {}

