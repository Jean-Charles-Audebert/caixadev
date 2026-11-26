package dev.caixadev.tools.cvcgaines.mcp;

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Protocole MCP (JSON-RPC sur HTTP, sans etat) du serveur cvc-gaines, avec une vraie cle d'API. */
@SpringBootTest(classes = HomeApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CvcGainesMcpHttpTest {

    private static final String MCP = "/cvc-gaines/mcp";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ApiKeyJpaRepository apiKeyJpaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final List<UUID> keys = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        keys.forEach(apiKeyJpaRepository::deleteById);
        keys.clear();
    }

    private String key(String scope) {
        ApiKeySecret.Generated g = ApiKeySecret.generate();
        ApiKey saved = apiKeyRepository.save(new ApiKey(UUID.randomUUID(), "mcp-test", g.keyPrefix(),
                ApiKeySecret.hash(g.rawKey()), List.of(scope), "test@example.test", Instant.now(), null, null, null));
        keys.add(saved.id());
        return g.rawKey();
    }

    private String rawKey;

    @BeforeEach
    void createKey() {
        rawKey = key("cvc-gaines");
    }

    @Test
    void initializeAdvertisesToolsAndInstructions() throws Exception {
        JsonNode r = rpc(rawKey, """
                {"jsonrpc": "2.0", "id": 1, "method": "initialize",
                 "params": {"protocolVersion": "2025-06-18", "capabilities": {},
                            "clientInfo": {"name": "test", "version": "1"}}}
                """, 200);
        JsonNode result = r.get("result");
        assertThat(result.get("serverInfo").get("name").asText()).isEqualTo("cvc-gaines");
        assertThat(result.get("capabilities").has("tools")).isTrue();
        assertThat(result.get("instructions").asText()).contains("m3/h");
        assertThat(result.get("protocolVersion").asText()).isNotBlank();
    }

    @Test
    void toolsListExposesSixReadOnlyToolsWithGeneratedSchemas() throws Exception {
        JsonNode tools = rpc(rawKey, """
                {"jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}}
                """, 200).get("result").get("tools");

        assertThat(tools).hasSize(6);
        List<String> names = new ArrayList<>();
        tools.forEach(t -> names.add(t.get("name").asText()));
        assertThat(names).contains("cvc_gaines_section_pressure_drop", "cvc_gaines_network_pressure_drop",
                "cvc_gaines_catalog");

        JsonNode section = null;
        for (JsonNode t : tools) {
            assertThat(t.get("annotations").get("readOnlyHint").asBoolean()).isTrue();
            if (t.get("name").asText().equals("cvc_gaines_section_pressure_drop")) {
                section = t;
            }
        }
        JsonNode schema = section.get("inputSchema");
        assertThat(schema.get("type").asText()).isEqualTo("object");
        assertThat(schema.get("properties").has("flowM3h")).isTrue();
        assertThat(schema.get("required").toString()).contains("flowM3h", "lengthM", "shape");
    }

    @Test
    void toolsCallReturnsStructuredResult() throws Exception {
        JsonNode result = rpc(rawKey, """
                {"jsonrpc": "2.0", "id": 3, "method": "tools/call",
                 "params": {"name": "cvc_gaines_section_pressure_drop",
                            "arguments": {"flowM3h": 2000, "lengthM": 10,
                                          "shape": {"kind": "CIRCULAR", "diameterMm": 315}}}}
                """, 200).get("result");

        assertThat(result.get("isError").asBoolean()).isFalse();
        assertThat(result.get("structuredContent").get("velocityMs").asDouble()).isCloseTo(7.129, within(0.001));
        assertThat(result.get("structuredContent").get("linearPressureDropPaPerM").asDouble()).isBetween(1.80, 1.90);
        assertThat(result.get("content").get(0).get("text").asText()).contains("velocityMs");
    }

    @Test
    void invalidArgumentsAndEngineErrorsAreToolErrors() throws Exception {
        JsonNode validation = rpc(rawKey, """
                {"jsonrpc": "2.0", "id": 4, "method": "tools/call",
                 "params": {"name": "cvc_gaines_workplace_fresh_air", "arguments": {"activity": "OFFICE", "occupants": 0}}}
                """, 200).get("result");
        assertThat(validation.get("isError").asBoolean()).isTrue();
        assertThat(validation.get("content").get(0).get("text").asText()).contains("occupants");

        JsonNode engine = rpc(rawKey, """
                {"jsonrpc": "2.0", "id": 5, "method": "tools/call",
                 "params": {"name": "cvc_gaines_duct_sizing",
                            "arguments": {"flowM3h": 1000000,
                                          "criteria": {"kind": "CIRCULAR", "maxVelocityMs": 5, "maxPressureDropPaPerM": 1}}}}
                """, 200).get("result");
        assertThat(engine.get("isError").asBoolean()).isTrue();
        assertThat(engine.get("content").get(0).get("text").asText()).contains("aucune dimension normalisee");

        JsonNode badShape = rpc(rawKey, """
                {"jsonrpc": "2.0", "id": 6, "method": "tools/call",
                 "params": {"name": "cvc_gaines_workplace_fresh_air", "arguments": {"activity": "SPACESHIP", "occupants": 1}}}
                """, 200).get("result");
        assertThat(badShape.get("isError").asBoolean()).isTrue();
    }

    @Test
    void endpointRequiresAKeyWithTheRightScope() throws Exception {
        String body = """
                {"jsonrpc": "2.0", "id": 7, "method": "tools/list", "params": {}}
                """;
        assertThat(post(null, body).getStatusCode().value()).isEqualTo(401);
        assertThat(post("cxd_AAAAAAAA_" + "B".repeat(44), body).getStatusCode().value()).isEqualTo(401);
        assertThat(post(key("autre-outil"), body).getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void getIsNotSupportedByTheStatelessTransport() {
        // Un client MCP tente parfois d'ouvrir un flux SSE en GET : il doit recevoir 405,
        // pas un 401 qui lui ferait croire que sa cle est refusee.
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(rawKey);
        headers.setAccept(List.of(MediaType.TEXT_EVENT_STREAM));
        ResponseEntity<String> response = rest.exchange(MCP, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(405);
    }

    private JsonNode rpc(String apiKey, String body, int expectedStatus) throws Exception {
        ResponseEntity<String> response = post(apiKey, body);
        assertThat(response.getStatusCode().value()).as(response.getBody()).isEqualTo(expectedStatus);
        String payload = response.getBody();
        // Le transport peut repondre en JSON ou en flux SSE d'un seul evenement.
        if (payload != null && payload.contains("data:")) {
            payload = payload.lines().filter(l -> l.startsWith("data:")).findFirst().orElseThrow().substring(5).trim();
        }
        return objectMapper.readTree(payload);
    }

    private ResponseEntity<String> post(String apiKey, String body) {
        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null) {
            headers.setBearerAuth(apiKey);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM));
        return rest.exchange(MCP, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }
}
