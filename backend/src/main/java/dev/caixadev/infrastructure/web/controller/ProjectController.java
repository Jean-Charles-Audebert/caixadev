package dev.caixadev.infrastructure.web.controller;

import dev.caixadev.application.dto.response.ProjectResponse;
import dev.caixadev.domain.model.Project;
import dev.caixadev.domain.port.in.ProjectUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectUseCase projectUseCase;

    /** Projets publics — accessible sans authentification. */
    @GetMapping("/public")
    public ResponseEntity<List<ProjectResponse>> listPublicProjects() {
        List<ProjectResponse> responses = projectUseCase.listPublicProjects().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    private ProjectResponse toResponse(Project p) {
        return new ProjectResponse(p.id(), p.key(), p.name(), p.description(), p.isActive(), p.isPublic(), List.of());
    }
}

