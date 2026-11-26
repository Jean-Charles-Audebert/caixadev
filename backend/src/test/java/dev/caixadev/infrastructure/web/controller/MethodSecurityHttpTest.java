package dev.caixadev.infrastructure.web.controller;

import dev.caixadev.domain.model.User;
import dev.caixadev.home.HomeApplication;
import dev.caixadev.infrastructure.persistence.entity.UserEntity;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie le refus de sécurité de méthode (@PreAuthorize) à travers la vraie
 * chaîne HTTP.
 *
 * La règle d'URL `/api/admin/**` masquerait ce chemin : le 403 serait alors
 * produit par le filtre de sécurité. Ici le contrôleur est hors de cette
 * règle, donc le 403 ne peut venir que de la conversion, par
 * {@code GlobalExceptionHandler}, de l'{@code AccessDeniedException} levée
 * pendant l'invocation du handler MVC. Sans ce handler, le catch-all
 * transformerait le refus en 500.
 */
@SpringBootTest(
        classes = HomeApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class MethodSecurityHttpTest {

    private static final String PROBE = "/api/test/method-security";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private JwtService jwtService;

    private final List<Long> createdUserIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        transactionTemplate.executeWithoutResult(status ->
                createdUserIds.forEach(userJpaRepository::deleteById));
        createdUserIds.clear();
    }

    @Test
    void methodSecurityDenialIsConvertedTo403Not500() {
        UserEntity user = createUser(false);

        ResponseEntity<String> response = restTemplate.exchange(
                PROBE, HttpMethod.GET, new HttpEntity<>(accessTokenHeaders(user)), String.class);

        assertThat(response.getStatusCode().value())
                .as("un refus @PreAuthorize doit être un 403, pas une erreur serveur")
                .isEqualTo(403);
        assertThat(response.getBody()).contains("FORBIDDEN", "Accès refusé");
    }

    @Test
    void superAdminPassesMethodSecurity() {
        UserEntity user = createUser(true);

        ResponseEntity<String> response = restTemplate.exchange(
                PROBE, HttpMethod.GET, new HttpEntity<>(accessTokenHeaders(user)), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("ok");
    }

    private UserEntity createUser(boolean superAdmin) {
        UserEntity user = transactionTemplate.execute(status -> userJpaRepository.save(
                UserEntity.builder()
                        .email("method-security-" + UUID.randomUUID() + "@example.test")
                        .password("not-used")
                        .displayName("Utilisateur méthode")
                        .isSuperAdmin(superAdmin)
                        .build()
        ));
        assertThat(user).isNotNull();
        createdUserIds.add(user.getId());
        return user;
    }

    private HttpHeaders accessTokenHeaders(UserEntity entity) {
        User domain = new User(
                entity.getId(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getDisplayName(),
                entity.isSuperAdmin(),
                entity.getCreatedAt()
        );
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "access_token=" + jwtService.generateAccessToken(domain));
        return headers;
    }
}
