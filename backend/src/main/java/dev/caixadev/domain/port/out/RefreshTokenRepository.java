package dev.caixadev.domain.port.out;

import dev.caixadev.domain.model.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {
    void save(RefreshToken token);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void revokeByTokenHash(String tokenHash);
    void revokeAllByUserId(Long userId);
}

