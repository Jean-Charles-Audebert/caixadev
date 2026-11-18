package dev.caixadev.infrastructure.persistence.repository;

import dev.caixadev.infrastructure.persistence.entity.UserProjectRoleEntity;
import dev.caixadev.infrastructure.persistence.entity.UserProjectRoleId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserProjectRoleJpaRepository extends JpaRepository<UserProjectRoleEntity, UserProjectRoleId> {

    /**
     * Charge l'affectation avec ses associations en une seule requête.
     * Sans cet {@link EntityGraph}, les associations LAZY restent non initialisées
     * et la conversion en domaine échoue hors transaction (LazyInitializationException).
     */
    @EntityGraph(attributePaths = {"user", "project", "role"})
    List<UserProjectRoleEntity> findByUserId(Long userId);
}
