package dev.caixadev.infrastructure.web.controller;

import dev.caixadev.application.dto.request.LoginRequest;
import dev.caixadev.application.dto.response.AuthResponse;
import dev.caixadev.domain.exception.InvalidTokenException;
import dev.caixadev.domain.model.LoginResult;
import dev.caixadev.domain.port.in.AuthUseCase;
import dev.caixadev.infrastructure.security.CookieUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;
    private final CookieUtils cookieUtils;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        LoginResult result = authUseCase.login(request.email(), request.password());
        cookieUtils.setAccessTokenCookie(response, result.accessToken());
        cookieUtils.setRefreshTokenCookie(response, result.rawRefreshToken());
        return ResponseEntity.ok(AuthResponse.from(result.user()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String rawRefreshToken = extractRefreshToken(request);
        if (rawRefreshToken == null) {
            throw new InvalidTokenException();
        }
        LoginResult result = authUseCase.refresh(rawRefreshToken);
        cookieUtils.setAccessTokenCookie(response, result.accessToken());
        cookieUtils.setRefreshTokenCookie(response, result.rawRefreshToken());
        return ResponseEntity.ok(AuthResponse.from(result.user()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String rawRefreshToken = extractRefreshToken(request);
        if (rawRefreshToken != null) {
            authUseCase.logout(rawRefreshToken);
        }
        cookieUtils.clearAllTokenCookies(response);
        return ResponseEntity.noContent().build();
    }

    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}

