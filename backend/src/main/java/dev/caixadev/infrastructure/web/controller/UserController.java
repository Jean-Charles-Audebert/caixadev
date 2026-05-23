package dev.caixadev.infrastructure.web.controller;

import dev.caixadev.application.dto.response.ProjectResponse;
import dev.caixadev.application.dto.response.UserResponse;
import dev.caixadev.domain.model.ProjectAccess;
import dev.caixadev.domain.model.TokenClaims;
import dev.caixadev.domain.port.in.ProjectUseCase;
import dev.caixadev.domain.port.in.UserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;
    private final ProjectUseCase projectUseCase;

    @GetMapping
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal TokenClaims claims) {
        return ResponseEntity.ok(UserResponse.from(userUseCase.getUserById(claims.userId())));
    }

    @GetMapping("/projects")
    public ResponseEntity<List<ProjectResponse>> getMyProjects(@AuthenticationPrincipal TokenClaims claims) {
        List<ProjectAccess> accesses = projectUseCase.listAccessibleProjects(claims.userId());
        List<ProjectResponse> responses = accesses.stream()
                .map(pa -> new ProjectResponse(
                        pa.project().id(),
                        pa.project().key(),
                        pa.project().name(),
                        pa.project().description(),
                        pa.project().isActive(),
                        pa.project().isPublic(),
                        pa.roles()
                ))
                .toList();
        return ResponseEntity.ok(responses);
    }
}

