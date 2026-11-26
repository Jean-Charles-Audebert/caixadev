package dev.caixadev.domain.port.in;

import dev.caixadev.domain.model.ApiKey;
import dev.caixadev.domain.model.ApiKeyCreation;
import dev.caixadev.domain.model.ApiKeyVerification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ApiKeyUseCase {
    /** Crée une clé ; la clé en clair n'est disponible que dans le résultat. */
    ApiKeyCreation create(String name, List<String> scopes, Instant expiresAt, String createdBy);

    List<ApiKey> list();

    /** Révoque la clé (idempotent : une clé déjà révoquée garde sa date d'origine). */
    void revoke(UUID id);

    /** Vérifie une clé présentée et, si elle est valide, met à jour sa dernière utilisation. */
    ApiKeyVerification authenticate(String rawKey);
}
