package dev.caixadev.infrastructure.security;

import dev.caixadev.domain.exception.InvalidTokenException;
import dev.caixadev.domain.model.TokenClaims;
import dev.caixadev.domain.model.User;
import dev.caixadev.domain.port.out.TokenPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtService implements TokenPort {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET est absent. Definissez la variable d'environnement JWT_SECRET "
                            + "(au moins 32 octets aleatoires) avant de demarrer l'application.");
        }
        // La cle doit faire au minimum 32 octets pour HS256
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET est trop court : 32 octets minimum sont requis pour HS256.");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    @Override
    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.id().toString())
                .claim("email", user.email())
                .claim("isSuperAdmin", user.isSuperAdmin())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public TokenClaims parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = Long.parseLong(claims.getSubject());
            String email = claims.get("email", String.class);
            boolean isSuperAdmin = Boolean.TRUE.equals(claims.get("isSuperAdmin", Boolean.class));

            return new TokenClaims(userId, email, isSuperAdmin);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token JWT invalide : {}", e.getMessage());
            throw new InvalidTokenException();
        }
    }
}


