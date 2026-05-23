package dev.caixadev.infrastructure.web.controller;

import dev.caixadev.application.dto.request.AssignRoleRequest;
import dev.caixadev.application.dto.request.CreateUserRequest;
import dev.caixadev.application.dto.request.UpdateUserRequest;
import dev.caixadev.application.dto.response.ProjectResponse;
import dev.caixadev.application.dto.response.UserResponse;
import dev.caixadev.domain.model.Project;
import dev.caixadev.domain.port.in.ProjectUseCase;
import dev.caixadev.domain.port.in.UserUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserUseCase userUseCase;
    private final ProjectUseCase projectUseCase;

    // ─── Utilisateurs ─────────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(userUseCase.listUsers().stream().map(UserResponse::from).toList());
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponse.from(userUseCase.createUser(req.email(), req.password(), req.displayName())));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest req
    ) {
        return ResponseEntity.ok(UserResponse.from(userUseCase.updateUser(id, req.displayName())));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userUseCase.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{id}/roles")
    public ResponseEntity<Void> assignRole(
            @PathVariable Long id,
            @Valid @RequestBody AssignRoleRequest req
    ) {
        userUseCase.assignRole(id, req.projectId(), req.roleId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{id}/roles")
    public ResponseEntity<Void> removeRole(
            @PathVariable Long id,
            @Valid @RequestBody AssignRoleRequest req
    ) {
        userUseCase.removeRole(id, req.projectId(), req.roleId());
        return ResponseEntity.noContent().build();
    }

    // ─── Projets ──────────────────────────────────────────────────────────────

    @GetMapping("/projects")
    public ResponseEntity<List<ProjectResponse>> listAllProjects() {
        return ResponseEntity.ok(projectUseCase.listAllProjects().stream().map(this::toResponse).toList());
    }

    private ProjectResponse toResponse(Project p) {
        return new ProjectResponse(p.id(), p.key(), p.name(), p.description(), p.isActive(), p.isPublic(), List.of());
    }
}

