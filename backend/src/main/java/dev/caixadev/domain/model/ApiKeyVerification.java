package dev.caixadev.domain.model;

/**
 * Résultat de la vérification d'une clé présentée. {@code keyPrefix} n'est
 * renseigné que si la clé est bien formée : il peut être journalisé, pas le secret.
 */
public record ApiKeyVerification(Status status, String keyPrefix, ApiKey apiKey) {

    public enum Status { VALID, MALFORMED, UNKNOWN, INVALID_SECRET, REVOKED, EXPIRED }

    public boolean isValid() {
        return status == Status.VALID;
    }

    public static ApiKeyVerification failure(Status status, String keyPrefix) {
        return new ApiKeyVerification(status, keyPrefix, null);
    }

    public static ApiKeyVerification valid(ApiKey apiKey) {
        return new ApiKeyVerification(Status.VALID, apiKey.keyPrefix(), apiKey);
    }
}
