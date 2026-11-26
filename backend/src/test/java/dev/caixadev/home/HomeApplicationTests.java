package dev.caixadev.home;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test d'intégration du contexte Spring.
 * Nécessite une base PostgreSQL active (voir CI avec service postgres).
 * En local : docker-compose up -d avant de lancer.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.flyway.enabled=true"
})
class HomeApplicationTests {

    @Test
    void contextLoads() {
        // Vérifie que le contexte Spring démarre sans erreur
    }
}
