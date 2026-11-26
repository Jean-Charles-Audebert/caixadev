-- =========================
-- API_KEYS : clés d'accès machine (Claude, scripts, MCP)
-- =========================
-- Format de la clé : cxd_<préfixe 8 car.>_<secret base62>.
-- Seule l'empreinte SHA-256 (hex) de la clé complète est stockée : la clé en
-- clair n'est restituée qu'une fois, à la création. Le préfixe, unique, sert
-- à retrouver la ligne sans exposer le secret (journaux, interface).
-- `scopes` : liste séparée par des virgules (ex. « cvc-gaines »), chaque
-- portée donnant l'autorité SCOPE_<portée>.
CREATE TABLE api_keys (
    id           UUID         PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    key_prefix   VARCHAR(8)   NOT NULL,
    key_hash     CHAR(64)     NOT NULL,
    scopes       TEXT         NOT NULL,
    created_by   VARCHAR(255) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at   TIMESTAMPTZ  NULL,
    last_used_at TIMESTAMPTZ  NULL,
    revoked_at   TIMESTAMPTZ  NULL,

    CONSTRAINT uk_api_keys_prefix UNIQUE (key_prefix)
);
