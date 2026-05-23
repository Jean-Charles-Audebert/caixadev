package dev.caixadev.infrastructure.persistence.repository;

import dev.caixadev.infrastructure.persistence.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByProjectIsNullAndName(String name);
}

