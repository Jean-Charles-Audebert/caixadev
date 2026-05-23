package dev.caixadev.domain.port.out;

import dev.caixadev.domain.model.Role;

import java.util.Optional;

public interface RoleRepository {
    Optional<Role> findById(Long id);
}

