package dev.caixadev.domain.service;

import dev.caixadev.domain.model.Project;
import dev.caixadev.domain.model.ProjectAccess;
import dev.caixadev.domain.model.User;
import dev.caixadev.domain.model.UserProjectRole;
import dev.caixadev.domain.port.in.ProjectUseCase;
import dev.caixadev.domain.port.out.ProjectRepository;
import dev.caixadev.domain.port.out.UserProjectRoleRepository;
import dev.caixadev.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService implements ProjectUseCase {

    private final ProjectRepository projectRepository;
    private final UserProjectRoleRepository userProjectRoleRepository;
    private final UserRepository userRepository;

    @Override
    public List<Project> listPublicProjects() {
        return projectRepository.findAllPublicAndActive();
    }

    @Override
    public List<ProjectAccess> listAccessibleProjects(Long userId) {
        List<UserProjectRole> assignments = userProjectRoleRepository.findByUserId(userId);

        // Grouper par projet et collecter les noms de rôles réellement attribués.
        Map<Long, List<String>> rolesByProject = assignments.stream()
                .collect(Collectors.groupingBy(
                        upr -> upr.project().id(),
                        Collectors.mapping(upr -> upr.role().name(), Collectors.toList())
                ));

        // Le SUPER_ADMIN est un rôle global de plateforme : il accède à tous les
        // projets actifs, qu'une affectation explicite existe ou non. Les rôles
        // renvoyés restent ceux réellement attribués sur le projet : le rôle
        // global n'est pas un rôle de projet et n'est donc jamais inventé ici.
        if (isSuperAdmin(userId)) {
            return projectRepository.findAllActive().stream()
                    .map(project -> new ProjectAccess(
                            project, rolesByProject.getOrDefault(project.id(), List.of())))
                    .toList();
        }

        return assignments.stream()
                .map(UserProjectRole::project)
                .filter(Project::isActive)
                .distinct()
                .map(project -> new ProjectAccess(project, rolesByProject.getOrDefault(project.id(), List.of())))
                .toList();
    }

    /**
     * Un identifiant inconnu n'est pas superadministrateur : le comportement
     * antérieur (liste vide pour un utilisateur sans affectation) est conservé
     * pour un jeton dont le compte aurait été supprimé entre-temps.
     */
    private boolean isSuperAdmin(Long userId) {
        return userRepository.findById(userId)
                .map(User::isSuperAdmin)
                .orElse(false);
    }

    @Override
    public List<Project> listAllProjects() {
        return projectRepository.findAllActive();
    }
}


