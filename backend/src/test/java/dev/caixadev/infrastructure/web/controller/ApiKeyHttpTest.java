package dev.caixadev.infrastructure.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.caixadev.domain.model.ApiKey;
import dev.caixadev.domain.model.ApiKeySecret;
import dev.caixadev.domain.model.User;
import dev.caixadev.domain.port.out.ApiKeyRepository;
import dev.caixadev.home.HomeApplication;
import dev.caixadev.infrastructure.persistence.entity.UserEntity;
import dev.caixadev.infrastructure.persistence.repository.ApiKeyJpaRepository;
import dev.caixadev.infrastructure.persistence.repository.UserJpaRepository;
import dev.caixadev.infrastructure.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests HTTP réels des clés API machine et de leur administration.
 *
 * La chaîne « clé API » est éprouvée sur {@code /cvc-gaines/v1/_probe}
 * ({@link ApiKeyProbeController}, sources de test uniquement) ; la chaîne
 * « cookie » sur {@code /api/users/me} et {@code /api/admin/api-keys}.
 */
@SpringBootTest(
        classes = HomeApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class ApiKeyHttpTest {

    private static final String PROBE = "/cvc-gaines/v1/_probe";
    private static final String ADMIN = "/api/admin/api-keys";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private ApiKeyJpaRepository apiKeyJpaRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<UUID> createdKeyIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        transactionTemplate.executeWithoutResult(status -> {
            createdKeyIds.forEach(apiKeyJpaRepository::deleteById);
            createdUserIds.forEach(userJpaRepository::deleteById);
        });
        createdKeyIds.clear();
        createdUserIds.clear();
    }

    // ─── Chaîne « clé API » ───────────────────────────────────────────────────

    @Test
    void missingKeyReturns401() {
        ResponseEntity<String> response = restTemplate.getForEntity(PROBE, String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).contains("UNAUTHORIZED");
    }

    @Test
    void malformedKeyReturns401() {
        for (String header : List.of(
                "Bearer not-a-key",
                "Bearer cxd_short_secret",
                "Basic dXNlcjpwYXNz",
                "cxd_" + "A".repeat(8) + "_" + "B".repeat(44))) {
            ResponseEntity<String> response = get(PROBE, authorization(header));
            assertThat(response.getStatusCode().value()).as(header).isEqualTo(401);
        }
    }

    @Test
    void unknownOrWrongSecretKeyReturns401() {
        ApiKeySecret.Generated unknown = ApiKeySecret.generate();
        assertThat(get(PROBE, bearer(unknown.rawKey())).getStatusCode().value()).isEqualTo(401);

        // Préfixe connu, secret différent : l'empreinte ne correspond pas.
        Stored stored = insertKey(List.of("cvc-gaines"), null, null);
        String forged = "cxd_" + stored.prefix() + "_" + "x".repeat(ApiKeySecret.SECRET_LENGTH);
        assertThat(get(PROBE, bearer(forged)).getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void validKeyReturns200AndRecordsLastUse() {
        Stored stored = insertKey(List.of("cvc-gaines"), Instant.now().plus(1, ChronoUnit.DAYS), null);

        ResponseEntity<String> response = get(PROBE, bearer(stored.rawKey()));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(stored.prefix());
        assertThat(apiKeyRepository.findById(stored.id()).orElseThrow().lastUsedAt())
                .as("la dernière utilisation doit être enregistrée")
                .isNotNull();
    }

    @Test
    void revokedKeyReturns401() {
        String superAdminCookie = cookieFor(createUser(true));
        JsonNode created = createViaAdmin(superAdminCookie, "{\"name\":\"révocable\",\"scopes\":[\"cvc-gaines\"]}");
        String rawKey = created.get("key").asText();
        String id = created.get("apiKey").get("id").asText();
        assertThat(get(PROBE, bearer(rawKey)).getStatusCode().value()).isEqualTo(200);

        ResponseEntity<String> revoke = restTemplate.exchange(
                ADMIN + "/" + id, HttpMethod.DELETE, new HttpEntity<>(cookie(superAdminCookie)), String.class);
        assertThat(revoke.getStatusCode().value()).isEqualTo(204);

        assertThat(get(PROBE, bearer(rawKey)).getStatusCode().value()).isEqualTo(401);
        assertThat(apiKeyRepository.findById(UUID.fromString(id)).orElseThrow().revokedAt()).isNotNull();
    }

    @Test
    void expiredKeyReturns401() {
        Stored stored = insertKey(List.of("cvc-gaines"), Instant.now().minus(1, ChronoUnit.MINUTES), null);

        assertThat(get(PROBE, bearer(stored.rawKey())).getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void keyWithoutRequiredScopeReturns403() {
        Stored stored = insertKey(List.of("autre-outil"), null, null);

        ResponseEntity<String> response = get(PROBE, bearer(stored.rawKey()));

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).contains("FORBIDDEN");
    }

    @Test
    void sessionCookieIsNeverAcceptedOnApiKeyChain() {
        String superAdminCookie = cookieFor(createUser(true));

        ResponseEntity<String> response = get(PROBE, cookie(superAdminCookie));

        assertThat(response.getStatusCode().value())
                .as("le cookie access_token ne doit jamais ouvrir /cvc-gaines")
                .isEqualTo(401);
    }

    @Test
    void cookieDoesNotOverrideApiKeyIdentity() {
        Stored stored = insertKey(List.of("cvc-gaines"), null, null);
        HttpHeaders headers = bearer(stored.rawKey());
        headers.add(HttpHeaders.COOKIE, "access_token=" + cookieFor(createUser(true)));

        ResponseEntity<String> response = get(PROBE, headers);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(stored.prefix());
    }

    // ─── Chaîne « cookie » ────────────────────────────────────────────────────

    @Test
    void apiKeyIsNeverAcceptedOnCookieChain() {
        Stored stored = insertKey(List.of("cvc-gaines"), null, null);

        assertThat(get("/api/users/me", bearer(stored.rawKey())).getStatusCode().value())
                .as("une clé API ne doit jamais ouvrir l'application à cookie")
                .isEqualTo(401);
        assertThat(get(ADMIN, bearer(stored.rawKey())).getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void adminEndpointsAreForbiddenForNormalUser() {
        String userCookie = cookieFor(createUser(false));
        Stored stored = insertKey(List.of("cvc-gaines"), null, null);

        assertThat(get(ADMIN, cookie(userCookie)).getStatusCode().value()).isEqualTo(403);
        assertThat(postJson(ADMIN, userCookie, "{\"name\":\"x\",\"scopes\":[\"cvc-gaines\"]}")
                .getStatusCode().value()).isEqualTo(403);
        assertThat(restTemplate.exchange(ADMIN + "/" + stored.id(), HttpMethod.DELETE,
                new HttpEntity<>(cookie(userCookie)), String.class).getStatusCode().value()).isEqualTo(403);
        assertThat(apiKeyRepository.findById(stored.id()).orElseThrow().revokedAt()).isNull();
    }

    @Test
    void adminEndpointsRequireAuthentication() {
        assertThat(restTemplate.getForEntity(ADMIN, String.class).getStatusCode().value()).isEqualTo(401);
    }

    // ─── Administration ───────────────────────────────────────────────────────

    @Test
    void createdKeyIsShownOnceAndNeverListed() throws Exception {
        String superAdminCookie = cookieFor(createUser(true));
        String expiresAt = Instant.now().plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();

        ResponseEntity<String> created = postJson(ADMIN, superAdminCookie,
                "{\"name\":\"Claude\",\"scopes\":[\"cvc-gaines\"],\"expiresAt\":\"" + expiresAt + "\"}");

        assertThat(created.getStatusCode().value()).isEqualTo(201);
        assertThat(created.getHeaders().getCacheControl()).contains("no-store");
        JsonNode body = objectMapper.readTree(created.getBody());
        String rawKey = body.get("key").asText();
        JsonNode apiKey = body.get("apiKey");
        createdKeyIds.add(UUID.fromString(apiKey.get("id").asText()));
        assertThat(rawKey).matches("^cxd_[0-9A-Za-z]{8}_[0-9A-Za-z]{44}$");
        assertThat(rawKey).startsWith("cxd_" + apiKey.get("keyPrefix").asText() + "_");
        assertThat(apiKey.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(apiKey.get("expiresAt").asText()).isEqualTo(expiresAt);
        assertThat(apiKey.has("keyHash")).isFalse();

        // La base ne garde que l'empreinte.
        ApiKey stored = apiKeyRepository.findById(UUID.fromString(apiKey.get("id").asText())).orElseThrow();
        assertThat(stored.keyHash()).isEqualTo(ApiKeySecret.hash(rawKey)).isNotEqualTo(rawKey);

        ResponseEntity<String> list = get(ADMIN, cookie(superAdminCookie));
        assertThat(list.getStatusCode().value()).isEqualTo(200);
        assertThat(list.getBody())
                .contains(apiKey.get("id").asText())
                .doesNotContain(rawKey)
                .doesNotContain(rawKey.substring(rawKey.lastIndexOf('_') + 1))
                .doesNotContain(stored.keyHash())
                .doesNotContain("\"key\"")
                .doesNotContain("keyHash");
    }

    @Test
    void createRejectsUnknownScopeAndPastExpiry() {
        String superAdminCookie = cookieFor(createUser(true));
        String past = Instant.now().minus(1, ChronoUnit.HOURS).toString();

        assertThat(postJson(ADMIN, superAdminCookie, "{\"name\":\"x\",\"scopes\":[\"inconnue\"]}")
                .getStatusCode().value()).isEqualTo(400);
        assertThat(postJson(ADMIN, superAdminCookie, "{\"name\":\"x\",\"scopes\":[]}")
                .getStatusCode().value()).isEqualTo(400);
        assertThat(postJson(ADMIN, superAdminCookie,
                "{\"name\":\"x\",\"scopes\":[\"cvc-gaines\"],\"expiresAt\":\"" + past + "\"}")
                .getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void revokeUnknownKeyReturns404() {
        String superAdminCookie = cookieFor(createUser(true));

        ResponseEntity<String> response = restTemplate.exchange(ADMIN + "/" + UUID.randomUUID(),
                HttpMethod.DELETE, new HttpEntity<>(cookie(superAdminCookie)), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    // ─── Outils ───────────────────────────────────────────────────────────────

    private record Stored(UUID id, String prefix, String rawKey) {
    }

    /** Insère une clé directement (permet portées inconnues et expiration passée). */
    private Stored insertKey(List<String> scopes, Instant expiresAt, Instant revokedAt) {
        ApiKeySecret.Generated generated = ApiKeySecret.generate();
        ApiKey saved = apiKeyRepository.save(new ApiKey(
                UUID.randomUUID(), "test", generated.keyPrefix(), ApiKeySecret.hash(generated.rawKey()),
                scopes, "test@example.test", Instant.now(), expiresAt, null, revokedAt));
        createdKeyIds.add(saved.id());
        return new Stored(saved.id(), generated.keyPrefix(), generated.rawKey());
    }

    private JsonNode createViaAdmin(String accessToken, String body) {
        ResponseEntity<String> response = postJson(ADMIN, accessToken, body);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        try {
            JsonNode node = objectMapper.readTree(response.getBody());
            createdKeyIds.add(UUID.fromString(node.get("apiKey").get("id").asText()));
            return node;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private UserEntity createUser(boolean superAdmin) {
        UserEntity user = transactionTemplate.execute(status -> userJpaRepository.save(
                UserEntity.builder()
                        .email("api-key-" + UUID.randomUUID() + "@example.test")
                        .password("not-used")
                        .displayName("Utilisateur clé API")
                        .isSuperAdmin(superAdmin)
                        .build()
        ));
        assertThat(user).isNotNull();
        createdUserIds.add(user.getId());
        return user;
    }

    private String cookieFor(UserEntity entity) {
        return jwtService.generateAccessToken(new User(
                entity.getId(), entity.getEmail(), entity.getPassword(),
                entity.getDisplayName(), entity.isSuperAdmin(), entity.getCreatedAt()));
    }

    private static HttpHeaders cookie(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "access_token=" + accessToken);
        return headers;
    }

    private static HttpHeaders bearer(String rawKey) {
        return authorization("Bearer " + rawKey);
    }

    private static HttpHeaders authorization(String value) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, value);
        return headers;
    }

    private ResponseEntity<String> get(String path, HttpHeaders headers) {
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private ResponseEntity<String> postJson(String path, String accessToken, String body) {
        HttpHeaders headers = cookie(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }
}
