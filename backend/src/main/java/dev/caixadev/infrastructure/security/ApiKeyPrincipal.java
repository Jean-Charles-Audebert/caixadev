package dev.caixadev.infrastructure.security;

import java.util.List;
import java.util.UUID;

/** Identité d'un client machine authentifié par clé API (jamais la clé elle-même). */
public record ApiKeyPrincipal(UUID id, String name, String keyPrefix, List<String> scopes) {
}
