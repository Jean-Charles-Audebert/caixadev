package dev.caixadev.domain.service;

import dev.caixadev.domain.model.Project;
import dev.caixadev.domain.model.ProjectAccess;
import dev.caixadev.domain.model.UserProjectRole;
import dev.caixadev.domain.port.in.ProjectUseCase;
import dev.caixadev.domain.port.out.ProjectRepository;
import dev.caixadev.domain.port.out.UserProjectRoleRepository;
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

    @Override
    public List<Project> listPublicProjects() {
        return projectRepository.findAllPublicAndActive();
    }

    @Override
    public List<ProjectAccess> listAccessibleProjects(Long userId) {
        List<UserProjectRole> assignments = userProjectRoleRepository.findByUserId(userId);

        // Grouper par projet et collecter les noms de rôles
        Map<Long, List<String>> rolesByProject = assignments.stream()
                .collect(Collectors.groupingBy(
                        upr -> upr.project().id(),
                        Collectors.mapping(upr -> upr.role().name(), Collectors.toList())
                ));

        return assignments.stream()
                .map(UserProjectRole::project)
                .filter(Project::isActive)
                .distinct()
                .map(project -> new ProjectAccess(project, rolesByProject.getOrDefault(project.id(), List.of())))
                .toList();
    }

    @Override
    public List<Project> listAllProjects() {
        return projectRepository.findAllActive();
    }
}


