package dev.caixadev.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/** Création d'une clé API. {@code expiresAt} facultatif (ISO-8601, ex. 2027-01-01T00:00:00Z). */
public record CreateApiKeyRequest(
        @NotBlank @Size(max = 100) String name,
        @NotEmpty List<String> scopes,
        Instant expiresAt
) {}
