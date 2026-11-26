package dev.caixadev.infrastructure.web.controller;

import dev.caixadev.application.dto.request.CreateApiKeyRequest;
import dev.caixadev.application.dto.response.ApiKeyResponse;
import dev.caixadev.application.dto.response.CreatedApiKeyResponse;
import dev.caixadev.domain.model.TokenClaims;
import dev.caixadev.domain.port.in.ApiKeyUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Administration des clés API machine (superadministrateur, session à cookie).
 * La clé complète n'est renvoyée qu'une fois, dans la réponse de création.
 */
@RestController
@RequestMapping("/api/admin/api-keys")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class ApiKeyAdminController {

    private final ApiKeyUseCase apiKeyUseCase;

    @PostMapping
    public ResponseEntity<CreatedApiKeyResponse> create(
            @Valid @RequestBody CreateApiKeyRequest req,
            @AuthenticationPrincipal TokenClaims claims
    ) {
        var creation = apiKeyUseCase.create(req.name(), req.scopes(), req.expiresAt(), claims.email());
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(CreatedApiKeyResponse.from(creation));
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> list() {
        return ResponseEntity.ok(apiKeyUseCase.list().stream().map(ApiKeyResponse::from).toList());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable UUID id) {
        apiKeyUseCase.revoke(id);
        return ResponseEntity.noContent().build();
    }
}
