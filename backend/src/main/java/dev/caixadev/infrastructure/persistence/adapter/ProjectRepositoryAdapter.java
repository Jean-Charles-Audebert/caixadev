package dev.caixadev.infrastructure.persistence.adapter;

import dev.caixadev.domain.model.Project;
import dev.caixadev.domain.port.out.ProjectRepository;
import dev.caixadev.infrastructure.persistence.mapper.ProjectMapper;
import dev.caixadev.infrastructure.persistence.repository.ProjectJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProjectRepositoryAdapter implements ProjectRepository {

    private final ProjectJpaRepository jpa;
    private final ProjectMapper mapper;

    @Override
    public List<Project> findAllActive() {
        return jpa.findAllByIsActiveTrue().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Project> findAllPublicAndActive() {
        return jpa.findAllByIsActiveTrueAndIsPublicTrue().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Project> findById(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Project> findByKey(String key) {
        return jpa.findByKey(key).map(mapper::toDomain);
    }

    @Override
    public Project save(Project project) {
        return mapper.toDomain(jpa.save(mapper.toEntity(project)));
    }
}

