package dev.caixadev.application.dto.response;

import dev.caixadev.domain.model.ApiKeyCreation;

/**
 * Réponse de création : seule occasion où la clé complète est transmise.
 * Elle n'est ni stockée ni consultable ensuite.
 */
public record CreatedApiKeyResponse(ApiKeyResponse apiKey, String key) {

    public static CreatedApiKeyResponse from(ApiKeyCreation creation) {
        return new CreatedApiKeyResponse(ApiKeyResponse.from(creation.apiKey()), creation.rawKey());
    }

    @Override
    public String toString() {
        return "CreatedApiKeyResponse[apiKey=" + apiKey.id() + "]";
    }
}
