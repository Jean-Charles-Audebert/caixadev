package dev.caixadev.infrastructure.persistence.adapter;

import dev.caixadev.domain.model.RefreshToken;
import dev.caixadev.domain.port.out.RefreshTokenRepository;
import dev.caixadev.infrastructure.persistence.entity.RefreshTokenEntity;
import dev.caixadev.infrastructure.persistence.entity.UserEntity;
import dev.caixadev.infrastructure.persistence.mapper.RefreshTokenMapper;
import dev.caixadev.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import dev.caixadev.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;
    private final UserJpaRepository userJpa;
    private final RefreshTokenMapper mapper;

    @Override
    public void save(RefreshToken token) {
        UserEntity user = userJpa.getReferenceById(token.userId());
        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .user(user)
                .tokenHash(token.tokenHash())
                .expiresAt(token.expiresAt())
                .revoked(token.revoked())
                .build();
        jpa.save(entity);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void revokeByTokenHash(String tokenHash) {
        jpa.revokeByTokenHash(tokenHash);
    }

    @Override
    @Transactional
    public void revokeAllByUserId(Long userId) {
        jpa.revokeAllByUserId(userId);
    }
}

