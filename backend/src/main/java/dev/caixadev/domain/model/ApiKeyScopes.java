package dev.caixadev.domain.model;

import java.util.Set;

/**
 * Portées connues des clés API. Une portée donne l'autorité
 * {@code SCOPE_<portée>} ; la création d'une clé refuse toute portée inconnue.
 */
public final class ApiKeyScopes {

    public static final String CVC_GAINES = "cvc-gaines";

    public static final Set<String> KNOWN = Set.of(CVC_GAINES);

    private ApiKeyScopes() {
    }
}
