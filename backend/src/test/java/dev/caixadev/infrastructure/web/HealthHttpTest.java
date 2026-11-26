package dev.caixadev.infrastructure.web;

import dev.caixadev.home.HomeApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Endpoints de sante utilises par le healthcheck Docker (T12) : accessibles
 * sans authentification, sans aucun detail, et seuls exposes d'Actuator.
 */
@SpringBootTest(
        classes = HomeApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class HealthHttpTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthEndpointsAreUpWithoutAuthenticationAndWithoutDetails() {
        for (String path : new String[]{"/actuator/health", "/actuator/health/liveness",
                "/actuator/health/readiness"}) {
            ResponseEntity<String> response = restTemplate.getForEntity(path, String.class);

            assertThat(response.getStatusCode()).as(path).isEqualTo(HttpStatus.OK);
            // Statut seul : ni composants (base, disque), ni details, ni version.
            assertThat(response.getBody()).as(path)
                    .startsWith("{\"status\":\"UP\"")
                    .doesNotContain("components", "details", "db", "diskSpace");
        }
    }

    @Test
    void otherActuatorEndpointsAreNotExposed() {
        for (String path : new String[]{"/actuator", "/actuator/env", "/actuator/info",
                "/actuator/health/db"}) {
            ResponseEntity<String> response = restTemplate.getForEntity(path, String.class);

            assertThat(response.getStatusCode()).as(path).isNotEqualTo(HttpStatus.OK);
        }
    }
}
