package dev.caixadev.domain.port.in;

import dev.caixadev.domain.model.Project;
import dev.caixadev.domain.model.ProjectAccess;

import java.util.List;

public interface ProjectUseCase {
    /** Projets publics (non authentifié). */
    List<Project> listPublicProjects();

    /** Projets accessibles à un utilisateur (actifs + rôle attribué). */
    List<ProjectAccess> listAccessibleProjects(Long userId);

    /** Liste tous les projets (SuperAdmin uniquement). */
    List<Project> listAllProjects();
}

