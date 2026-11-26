package dev.caixadev.domain.model;

/** Résultat d'une création : la clé en clair n'existe que dans cet objet. */
public record ApiKeyCreation(ApiKey apiKey, String rawKey) {
    @Override
    public String toString() {
        return "ApiKeyCreation[apiKey=" + apiKey + "]";
    }
}
