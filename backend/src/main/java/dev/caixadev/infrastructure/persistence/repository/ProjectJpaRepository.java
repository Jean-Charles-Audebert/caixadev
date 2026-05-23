package dev.caixadev.infrastructure.persistence.repository;

import dev.caixadev.infrastructure.persistence.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectJpaRepository extends JpaRepository<ProjectEntity, Long> {
    Optional<ProjectEntity> findByKey(String key);
    List<ProjectEntity> findAllByIsActiveTrue();
    List<ProjectEntity> findAllByIsActiveTrueAndIsPublicTrue();
}

