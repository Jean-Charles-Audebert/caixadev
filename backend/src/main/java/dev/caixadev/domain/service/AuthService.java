package dev.caixadev.domain.service;

import dev.caixadev.domain.exception.InvalidCredentialsException;
import dev.caixadev.domain.exception.InvalidTokenException;
import dev.caixadev.domain.exception.UserNotFoundException;
import dev.caixadev.domain.model.LoginResult;
import dev.caixadev.domain.model.RefreshToken;
import dev.caixadev.domain.model.User;
import dev.caixadev.domain.port.in.AuthUseCase;
import dev.caixadev.domain.port.out.PasswordHashPort;
import dev.caixadev.domain.port.out.RefreshTokenRepository;
import dev.caixadev.domain.port.out.TokenPort;
import dev.caixadev.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenPort tokenPort;
    private final PasswordHashPort passwordHashPort;

    @Value("${jwt.refresh-token-expiration-days}")
    private int refreshTokenExpirationDays;

    @Override
    @Transactional
    public LoginResult login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHashPort.matches(password, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = tokenPort.generateAccessToken(user);
        String rawRefreshToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.create(user.id(), rawRefreshToken, refreshTokenExpirationDays);
        refreshTokenRepository.save(refreshToken);

        return new LoginResult(user, accessToken, rawRefreshToken);
    }

    @Override
    @Transactional
    public LoginResult refresh(String rawRefreshToken) {
        String hash = RefreshToken.hash(rawRefreshToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(InvalidTokenException::new);

        if (!stored.isValid()) {
            throw new InvalidTokenException();
        }

        refreshTokenRepository.revokeByTokenHash(hash);

        User user = userRepository.findById(stored.userId())
                .orElseThrow(() -> new UserNotFoundException(stored.userId()));

        String newAccessToken = tokenPort.generateAccessToken(user);
        String newRawRefreshToken = UUID.randomUUID().toString();
        RefreshToken newRefreshToken = RefreshToken.create(user.id(), newRawRefreshToken, refreshTokenExpirationDays);
        refreshTokenRepository.save(newRefreshToken);

        return new LoginResult(user, newAccessToken, newRawRefreshToken);
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        String hash = RefreshToken.hash(rawRefreshToken);
        refreshTokenRepository.revokeByTokenHash(hash);
    }
}

