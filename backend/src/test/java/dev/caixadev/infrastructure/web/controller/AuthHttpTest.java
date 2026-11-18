package dev.caixadev.infrastructure.web.controller;

import dev.caixadev.domain.port.out.PasswordHashPort;
import dev.caixadev.home.HomeApplication;
import dev.caixadev.infrastructure.persistence.entity.UserEntity;
import dev.caixadev.infrastructure.persistence.repository.UserJpaRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests HTTP réels du cycle de vie de l'authentification.
 *
 * Couvre : connexion, rotation du refresh token, réutilisation d'un ancien
 * token, logout sans access token valide, logout sans refresh token,
 * distinction 401 / 403 et corps JSON invalide.
 */
@SpringBootTest(
        classes = HomeApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class AuthHttpTest {

    private static final String PASSWORD = "Test1234!";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PasswordHashPort passwordHashPort;

    @Autowired
    private Environment environment;

    private Long createdUserId;

    @AfterEach
    void cleanUp() {
        if (createdUserId != null) {
            transactionTemplate.executeWithoutResult(status -> userJpaRepository.deleteById(createdUserId));
        }
    }

    @Test
    void loginIssuesAccessAndRefreshCookies() {
        UserEntity user = createUser();

        ResponseEntity<String> response = postJson("/api/auth/login", loginBody(user.getEmail()));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        String access = setCookie(response, "access_token", "/");
        String refresh = setCookie(response, "refresh_token", "/api/auth");
        assertThat(access).isNotNull().contains("Path=/", "HttpOnly", "Secure", "SameSite=Strict");
        assertThat(refresh).isNotNull().contains("Path=/api/auth", "HttpOnly", "Secure", "SameSite=Strict");

        // L'ancien cookie (chemin plus spécifique) est purgé à l'émission du nouveau :
        // sinon il serait envoyé en premier et masquerait le jeton valide.
        String legacy = setCookie(response, "refresh_token", "/api/auth/refresh");
        assertThat(legacy).isNotNull().contains("Max-Age=0", "Path=/api/auth/refresh");
    }

    @Test
    void refreshRotatesTokenAndOldTokenCannotBeReused() {
        UserEntity user = createUser();
        String rawRefreshToken = rawCookie(postJson("/api/auth/login", loginBody(user.getEmail())), "refresh_token", "/api/auth");
        assertThat(rawRefreshToken).isNotBlank();

        ResponseEntity<String> refreshResponse = postWithRefreshCookie("/api/auth/refresh", rawRefreshToken);

        assertThat(refreshResponse.getStatusCode().value()).isEqualTo(200);
        String rotated = rawCookie(refreshResponse, "refresh_token", "/api/auth");
        assertThat(rotated).isNotBlank().isNotEqualTo(rawRefreshToken);

        ResponseEntity<String> reuse = postWithRefreshCookie("/api/auth/refresh", rawRefreshToken);
        assertThat(reuse.getStatusCode().value()).isEqualTo(401);

        ResponseEntity<String> valid = postWithRefreshCookie("/api/auth/refresh", rotated);
        assertThat(valid.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void logoutWithoutValidAccessTokenRevokesRefreshToken() {
        UserEntity user = createUser();
        String rawRefreshToken = rawCookie(postJson("/api/auth/login", loginBody(user.getEmail())), "refresh_token", "/api/auth");

        // Aucun cookie d'access token : le logout doit rester possible.
        ResponseEntity<String> logout = postWithRefreshCookie("/api/auth/logout", rawRefreshToken);

        assertThat(logout.getStatusCode().value()).isEqualTo(204);
        String cleared = setCookie(logout, "refresh_token", "/api/auth");
        assertThat(cleared).isNotNull();
        assertThat(cleared).contains("Max-Age=0", "Path=/api/auth");

        ResponseEntity<String> afterLogout = postWithRefreshCookie("/api/auth/refresh", rawRefreshToken);
        assertThat(afterLogout.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void logoutWithoutRefreshTokenIsIdempotent() {
        ResponseEntity<String> logout = restTemplate.exchange(
                "/api/auth/logout",
                HttpMethod.POST,
                new HttpEntity<>(new HttpHeaders()),
                String.class
        );

        assertThat(logout.getStatusCode().value()).isEqualTo(204);
        assertThat(setCookie(logout, "access_token", "/")).contains("Max-Age=0");
        assertThat(setCookie(logout, "refresh_token", "/api/auth")).contains("Max-Age=0", "Path=/api/auth");
        assertThat(setCookie(logout, "refresh_token", "/api/auth/refresh"))
                .as("l'ancien cookie de refresh doit aussi être purgé")
                .contains("Max-Age=0", "Path=/api/auth/refresh");
    }

    @Test
    void logoutWithExpiredAccessTokenRevokesRefreshToken() {
        UserEntity user = createUser();
        String rawRefreshToken = rawCookie(
                postJson("/api/auth/login", loginBody(user.getEmail())), "refresh_token", "/api/auth");
        String expiredAccessToken = expiredAccessTokenFor(user);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE,
                "access_token=" + expiredAccessToken + "; refresh_token=" + rawRefreshToken);
        ResponseEntity<String> logout = restTemplate.exchange(
                "/api/auth/logout", HttpMethod.POST, new HttpEntity<>(headers), String.class);

        assertThat(logout.getStatusCode().value()).isEqualTo(204);
        assertThat(postWithRefreshCookie("/api/auth/refresh", rawRefreshToken).getStatusCode().value())
                .as("le refresh token doit être révoqué malgré l'access token expiré")
                .isEqualTo(401);
    }

    @Test
    void logoutRevokesOnlyThePresentedRefreshToken() {
        UserEntity user = createUser();
        String first = rawCookie(postJson("/api/auth/login", loginBody(user.getEmail())), "refresh_token", "/api/auth");
        String second = rawCookie(postJson("/api/auth/login", loginBody(user.getEmail())), "refresh_token", "/api/auth");
        assertThat(first).isNotEqualTo(second);

        assertThat(postWithRefreshCookie("/api/auth/logout", first).getStatusCode().value()).isEqualTo(204);

        assertThat(postWithRefreshCookie("/api/auth/refresh", first).getStatusCode().value()).isEqualTo(401);
        assertThat(postWithRefreshCookie("/api/auth/refresh", second).getStatusCode().value())
                .as("une autre session ne doit pas être révoquée")
                .isEqualTo(200);
    }

    @Test
    void unauthenticatedRequestReceives401() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/users/me", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).contains("UNAUTHORIZED");
    }

    @Test
    void authenticatedUserWithoutRoleReceives403() {
        UserEntity user = createUser();
        String access = rawCookie(postJson("/api/auth/login", loginBody(user.getEmail())), "access_token", "/");

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "access_token=" + access);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/users",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).contains("FORBIDDEN");
        assertThat(response.getBody()).doesNotContain("UNAUTHORIZED");
    }

    @Test
    void malformedJsonReturns400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                new HttpEntity<>("{\"email\": \"incomplete\"", headers),
                String.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains("MALFORMED_REQUEST");
        // Aucun détail interne (classe, pile, position Jackson) ne doit fuiter.
        assertThat(response.getBody()).doesNotContain("JsonParseException", "com.fasterxml");
    }

    private UserEntity createUser() {
        UserEntity user = transactionTemplate.execute(status -> userJpaRepository.save(
                UserEntity.builder()
                        .email("auth-" + UUID.randomUUID() + "@example.test")
                        .password(passwordHashPort.encode(PASSWORD))
                        .displayName("Utilisateur auth")
                        .isSuperAdmin(false)
                        .build()
        ));
        assertThat(user).isNotNull();
        createdUserId = user.getId();
        return user;
    }

    private String loginBody(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}";
    }

    private ResponseEntity<String> postJson(String path, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> postWithRefreshCookie(String path, String rawRefreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "refresh_token=" + rawRefreshToken);
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(headers), String.class);
    }

    private static List<String> setCookies(ResponseEntity<?> response) {
        List<String> values = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        return values == null ? List.of() : values;
    }

    private static String setCookie(ResponseEntity<?> response, String name, String path) {
        return setCookies(response).stream()
                .filter(header -> header.startsWith(name + "="))
                .filter(header -> hasExactPath(header, path))
                .findFirst()
                .orElse(null);
    }

    /**
     * Évite la confusion entre `Path=/api/auth` et `Path=/api/auth/refresh`
     * (l'un est un préfixe de l'autre).
     */
    private static boolean hasExactPath(String setCookieHeader, String path) {
        String normalized = setCookieHeader.replace(" ", "");
        return normalized.contains("Path=" + path + ";") || normalized.endsWith("Path=" + path);
    }

    private static String rawCookie(ResponseEntity<?> response, String name, String path) {
        String header = setCookie(response, name, path);
        if (header == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("^" + name + "=([^;]*)").matcher(header);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String expiredAccessTokenFor(UserEntity user) {
        String secret = environment.getProperty("jwt.secret");
        assertThat(secret).as("jwt.secret doit être configuré pour le profil test").isNotBlank();
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("isSuperAdmin", false)
                .issuedAt(new Date(now - 3_600_000))
                .expiration(new Date(now - 1_000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
