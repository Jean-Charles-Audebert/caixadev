package dev.caixadev.infrastructure.persistence.repository;

import dev.caixadev.infrastructure.persistence.entity.UserProjectRoleEntity;
import dev.caixadev.infrastructure.persistence.entity.UserProjectRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserProjectRoleJpaRepository extends JpaRepository<UserProjectRoleEntity, UserProjectRoleId> {
    List<UserProjectRoleEntity> findByUserId(Long userId);
}

