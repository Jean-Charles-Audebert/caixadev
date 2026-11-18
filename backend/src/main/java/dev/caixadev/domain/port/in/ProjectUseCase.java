package dev.caixadev.domain.port.in;

import dev.caixadev.domain.model.Project;
import dev.caixadev.domain.model.ProjectAccess;

import java.util.List;

public interface ProjectUseCase {
    /** Projets publics (non authentifié). */
    List<Project> listPublicProjects();

    /**
     * Projets accessibles à un utilisateur : projets actifs auxquels il est
     * affecté. Un SUPER_ADMIN voit tous les projets actifs, qu'une affectation
     * existe ou non (rôle global de plateforme).
     */
    List<ProjectAccess> listAccessibleProjects(Long userId);

    /** Liste tous les projets (SuperAdmin uniquement). */
    List<Project> listAllProjects();
}

