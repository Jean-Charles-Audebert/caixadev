package dev.caixadev.infrastructure.persistence.adapter;

import dev.caixadev.domain.model.Role;
import dev.caixadev.domain.port.out.RoleRepository;
import dev.caixadev.infrastructure.persistence.mapper.RoleMapper;
import dev.caixadev.infrastructure.persistence.repository.RoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepository {

    private final RoleJpaRepository jpa;
    private final RoleMapper mapper;

    @Override
    public Optional<Role> findById(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }
}

