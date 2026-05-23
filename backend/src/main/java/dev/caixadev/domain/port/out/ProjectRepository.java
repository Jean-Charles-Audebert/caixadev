package dev.caixadev.domain.port.out;

import dev.caixadev.domain.model.Project;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository {
    List<Project> findAllActive();
    List<Project> findAllPublicAndActive();
    Optional<Project> findById(Long id);
    Optional<Project> findByKey(String key);
    Project save(Project project);
}

