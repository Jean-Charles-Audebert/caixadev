package dev.caixadev.tools.cvcgaines.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.caixadev.domain.model.ApiKey;
import dev.caixadev.domain.model.ApiKeySecret;
import dev.caixadev.domain.port.out.ApiKeyRepository;
import dev.caixadev.home.HomeApplication;
import dev.caixadev.infrastructure.persistence.repository.ApiKeyJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Tests HTTP de l'API cvc-gaines v1, avec une vraie cle d'API. */
@SpringBootTest(classes = HomeApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CvcGainesHttpTest {

    private static final String BASE = "/cvc-gaines/v1";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ApiKeyJpaRepository apiKeyJpaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID keyId;
    private String rawKey;

    @BeforeEach
    void createKey() {
        ApiKeySecret.Generated generated = ApiKeySecret.generate();
        ApiKey saved = apiKeyRepository.save(new ApiKey(UUID.randomUUID(), "cvc-gaines-test", generated.keyPrefix(),
                ApiKeySecret.hash(generated.rawKey()), List.of("cvc-gaines"), "test@example.test", Instant.now(),
                null, null, null));
        keyId = saved.id();
        rawKey = generated.rawKey();
    }

    @AfterEach
    void deleteKey() {
        apiKeyJpaRepository.deleteById(keyId);
    }

    @Test
    void sectionPressureDropReproducesReferenceCase() throws Exception {
        JsonNode r = post("/sections/pressure-drop", """
                {"flowM3h": 2000, "lengthM": 10, "shape": {"kind": "CIRCULAR", "diameterMm": 315},
                 "fittings": [{"type": "ROUND_ELBOW", "radiusRatio": 1.0, "angleDeg": 90},
                              {"type": "FIXED_PRESSURE_DROP", "label": "Bouche", "pressureDropPa": 20}]}
                """, 200);

        assertThat(r.get("velocityMs").asDouble()).isCloseTo(7.129, within(0.001));
        assertThat(r.get("linearPressureDropPaPerM").asDouble()).isBetween(1.80, 1.90);
        assertThat(r.get("regime").asText()).isEqualTo("TURBULENT");
        double pd = r.get("dynamicPressurePa").asDouble();
        assertThat(r.get("singularPressureDropPa").asDouble()).isCloseTo(0.22 * pd + 20, within(0.01));
        assertThat(r.get("fittings")).hasSize(2);
        assertThat(r.get("equivalentDiameterMm").isNull()).isTrue();
    }

    @Test
    void rectangularSectionReportsEquivalentDiameter() throws Exception {
        JsonNode r = post("/sections/pressure-drop", """
                {"flowM3h": 1500, "lengthM": 5, "shape": {"kind": "RECTANGULAR", "widthMm": 400, "heightMm": 200},
                 "material": "GALVANIZED_SPIRAL", "air": {"temperatureC": 35, "altitudeM": 800}}
                """, 200);
        assertThat(r.get("equivalentDiameterMm").asDouble()).isCloseTo(305, within(1.0));
        assertThat(r.get("hydraulicDiameterMm").asDouble()).isCloseTo(266.7, within(0.1));
    }

    @Test
    void sizingReturnsStandardDiameter() throws Exception {
        JsonNode r = post("/sections/sizing", """
                {"flowM3h": 2000, "criteria": {"kind": "CIRCULAR", "maxVelocityMs": 7, "maxPressureDropPaPerM": 1.5}}
                """, 200);
        assertThat(r.get("shape").get("diameterMm").asDouble()).isEqualTo(355.0);
        assertThat(r.get("limitedBy").asText()).isEqualTo("vitesse maximale");
    }

    @Test
    void networkReturnsCriticalPathAndSizesMissingShapes() throws Exception {
        JsonNode r = post("/networks/pressure-drop", """
                {"sizing": {"kind": "CIRCULAR", "maxVelocityMs": 5, "maxPressureDropPaPerM": 1.0},
                 "sections": [
                   {"id": "A", "lengthM": 10},
                   {"id": "B", "parentId": "A", "lengthM": 5, "terminalFlowM3h": 600,
                    "fittings": [{"type": "FIXED_PRESSURE_DROP", "label": "Bouche", "pressureDropPa": 15}]},
                   {"id": "C", "parentId": "A", "lengthM": 20, "terminalFlowM3h": 400,
                    "shape": {"kind": "CIRCULAR", "diameterMm": 200},
                    "fittings": [{"type": "FIXED_PRESSURE_DROP", "label": "Bouche", "pressureDropPa": 15}]}]}
                """, 200);

        assertThat(r.get("totalFlowM3h").asDouble()).isEqualTo(1000.0);
        assertThat(r.get("sections").get(0).get("sized").asBoolean()).isTrue();
        assertThat(r.get("paths")).hasSize(2);
        String critical = r.get("criticalTerminalId").asText();
        for (JsonNode p : r.get("paths")) {
            if (p.get("terminalId").asText().equals(critical)) {
                assertThat(p.get("imbalancePa").asDouble()).isZero();
                assertThat(p.get("pressureDropPa").asDouble()).isEqualTo(r.get("criticalPressureDropPa").asDouble());
            } else {
                assertThat(p.get("imbalancePa").asDouble()).isPositive();
            }
        }
    }

    @Test
    void regulatoryFlows() throws Exception {
        JsonNode dwelling = post("/flows/dwelling", """
                {"mainRooms": 4, "bathrooms": 1, "toilets": 2}
                """, 200);
        assertThat(dwelling.get("totalExtractM3h").asInt()).isEqualTo(180);
        assertThat(dwelling.get("minimumTotalM3h").asInt()).isEqualTo(90);

        JsonNode office = post("/flows/workplace", """
                {"activity": "OFFICE", "occupants": 12}
                """, 200);
        assertThat(office.get("freshAirM3h").asInt()).isEqualTo(300);
    }

    @Test
    void engineInputErrorsAre400WithMessage() throws Exception {
        JsonNode cycle = post("/networks/pressure-drop", """
                {"sections": [{"id": "A", "lengthM": 1, "terminalFlowM3h": 100,
                               "shape": {"kind": "CIRCULAR", "diameterMm": 200}},
                              {"id": "B", "lengthM": 1, "terminalFlowM3h": 100,
                               "shape": {"kind": "CIRCULAR", "diameterMm": 200}}]}
                """, 400);
        assertThat(cycle.get("error").asText()).isEqualTo("CALCULATION_INPUT_ERROR");
        assertThat(cycle.get("message").asText()).contains("plusieurs troncons racines");

        JsonNode missingParam = post("/sections/pressure-drop", """
                {"flowM3h": 100, "lengthM": 1, "shape": {"kind": "CIRCULAR"}}
                """, 400);
        assertThat(missingParam.get("message").asText()).contains("shape.diameterMm");

        JsonNode validation = post("/sections/pressure-drop", """
                {"flowM3h": -5, "lengthM": 1, "shape": {"kind": "CIRCULAR", "diameterMm": 200}}
                """, 400);
        assertThat(validation.get("error").asText()).isEqualTo("VALIDATION_ERROR");

        JsonNode badEnum = post("/flows/workplace", """
                {"activity": "SPACESHIP", "occupants": 1}
                """, 400);
        assertThat(badEnum.get("error").asText()).isEqualTo("MALFORMED_REQUEST");
    }

    @Test
    void catalogAndOpenApiRequireTheKey() throws Exception {
        assertThat(rest.getForEntity(BASE + "/catalog", String.class).getStatusCode().value()).isEqualTo(401);
        assertThat(rest.getForEntity(BASE + "/openapi.json", String.class).getStatusCode().value()).isEqualTo(401);

        JsonNode catalog = get("/catalog");
        assertThat(catalog.get("circularDiametersMm")).isNotEmpty();
        assertThat(catalog.get("materials").get(0).get("roughnessMm").asDouble()).isPositive();

        JsonNode openApi = get("/openapi.json");
        assertThat(openApi.get("info").get("title").asText()).isEqualTo("cvc-gaines");
        assertThat(openApi.get("paths").has("/cvc-gaines/v1/sections/pressure-drop")).isTrue();
        // Les routes de la plateforme ne sont pas publiees.
        openApi.get("paths").fieldNames().forEachRemaining(p -> assertThat(p).startsWith("/cvc-gaines/v1/"));
    }

    private JsonNode post(String path, String json, int expectedStatus) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(rawKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = rest.exchange(BASE + path, HttpMethod.POST,
                new HttpEntity<>(json, headers), String.class);
        assertThat(response.getStatusCode().value()).as(response.getBody()).isEqualTo(expectedStatus);
        return objectMapper.readTree(response.getBody());
    }

    private JsonNode get(String path) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(rawKey);
        ResponseEntity<String> response = rest.exchange(BASE + path, HttpMethod.GET, new HttpEntity<>(headers),
                String.class);
        assertThat(response.getStatusCode().value()).as(response.getBody()).isEqualTo(200);
        return objectMapper.readTree(response.getBody());
    }
}
