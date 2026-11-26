package dev.caixadev.tools.cvcgaines.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import dev.caixadev.tools.cvcgaines.api.CvcGainesDtos.DwellingRequest;
import dev.caixadev.tools.cvcgaines.api.CvcGainesDtos.NetworkRequest;
import dev.caixadev.tools.cvcgaines.api.CvcGainesDtos.SectionRequest;
import dev.caixadev.tools.cvcgaines.api.CvcGainesDtos.SizingRequest;
import dev.caixadev.tools.cvcgaines.api.CvcGainesDtos.WorkplaceRequest;
import dev.caixadev.tools.cvcgaines.api.CvcGainesService;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Serveur MCP de {@code cvc-gaines} (T25), expose publiquement sur
 * {@code https://mcp.caixadev.dev/cvc-gaines} (Traefik reecrit le chemin en
 * {@value #ENDPOINT}).
 *
 * <ul>
 *   <li>Transport HTTP « streamable » <b>sans etat</b> du SDK officiel : les
 *       outils sont des calculs purs, aucune session n'est necessaire.</li>
 *   <li>Authentification : la meme cle d'API que l'API REST
 *       ({@code Authorization: Bearer cxd_...}, portee {@code cvc-gaines}),
 *       verifiee par la chaine de securite {@code /cvc-gaines/**} avant que la
 *       requete n'atteigne le servlet. Aucun cookie n'est accepte : pas de
 *       risque de requete forgee depuis un navigateur, d'ou l'absence de
 *       filtrage par {@code Origin}.</li>
 *   <li>Memes cas d'usage que l'API REST ({@link CvcGainesService}) et memes
 *       DTO : les schemas d'entree des outils sont generes depuis ces DTO
 *       (contraintes Jakarta et descriptions {@code @Schema} comprises).</li>
 * </ul>
 */
@Slf4j
@Configuration
public class CvcGainesMcpServer {

    static final String ENDPOINT = "/cvc-gaines/mcp";
    private static final int MAX_REQUEST_BYTES = 512 * 1024;

    private static final String INSTRUCTIONS = """
            Calculs aerauliques de reseaux de gaines (CVC). Unites dans chaque nom de champ : debits en m3/h \
            (flowM3h), longueurs en m (lengthM), dimensions en mm (diameterMm, widthMm, heightMm), pressions en Pa. \
            Air par defaut : 20 degC au niveau de la mer ; rugosite par defaut : acier galvanise 0,15 mm. \
            Appeler cvc_gaines_catalog pour connaitre les materiaux, les types d'accessoires et les series \
            normalisees. Les zeta d'accessoires sont rapportes a la pression dynamique du troncon qui les porte.""";

    @Bean
    public HttpServletStatelessServerTransport cvcGainesMcpTransport(ObjectMapper objectMapper) {
        return HttpServletStatelessServerTransport.builder()
                .jsonMapper(new JacksonMcpJsonMapper(objectMapper))
                .messageEndpoint(ENDPOINT)
                .maxRequestSize(MAX_REQUEST_BYTES)
                .build();
    }

    @Bean
    public ServletRegistrationBean<HttpServletStatelessServerTransport> cvcGainesMcpServlet(
            HttpServletStatelessServerTransport transport) {
        ServletRegistrationBean<HttpServletStatelessServerTransport> registration =
                new ServletRegistrationBean<>(transport, ENDPOINT);
        registration.setName("cvc-gaines-mcp");
        return registration;
    }

    @Bean(destroyMethod = "close")
    public McpStatelessSyncServer cvcGainesMcp(HttpServletStatelessServerTransport transport, ObjectMapper objectMapper,
                                               Validator validator, CvcGainesService service) {
        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(objectMapper);
        SchemaGenerator schemas = schemaGenerator();
        Tools tools = new Tools(objectMapper, validator, jsonMapper, schemas);

        return McpServer.sync(transport)
                .serverInfo("cvc-gaines", "1.0.0")
                .instructions(INSTRUCTIONS)
                .jsonMapper(jsonMapper)
                .capabilities(McpSchema.ServerCapabilities.builder().tools(false).build())
                // Validation par les contraintes Jakarta des DTO (messages en francais).
                .validateToolInputs(false)
                .tools(
                        tools.of("cvc_gaines_section_pressure_drop", "Perte de charge d'un troncon",
                                "Perte de charge d'un troncon de gaine a debit constant : vitesse, Reynolds, "
                                        + "coefficient de Darcy (Colebrook-White), pertes lineique et singulieres.",
                                SectionRequest.class, service::sectionPressureDrop),
                        tools.of("cvc_gaines_duct_sizing", "Dimensionnement d'une gaine",
                                "Plus petite dimension normalisee (NF EN 1506 circulaire, NF EN 1505 rectangulaire "
                                        + "a hauteur imposee) respectant la vitesse et la perte de charge lineique maximales.",
                                SizingRequest.class, service::sizing),
                        tools.of("cvc_gaines_network_pressure_drop", "Calcul d'un reseau de gaines",
                                "Reseau arborescent raccorde a un ventilateur : debits sommes depuis les terminaux, "
                                        + "perte de charge de chaque chemin, chemin critique (pression a fournir), "
                                        + "desequilibre a compenser par terminal ; dimensionne les troncons sans section "
                                        + "si sizing est fourni.",
                                NetworkRequest.class, service::network),
                        tools.of("cvc_gaines_dwelling_extract_flows", "Debits d'extraction d'un logement",
                                "Debits d'extraction reglementaires par piece de service et debits reduits minimaux "
                                        + "(arrete du 24 mars 1982 modifie, articles 3 et 4).",
                                DwellingRequest.class, service::dwelling),
                        tools.of("cvc_gaines_workplace_fresh_air", "Air neuf d'un local de travail",
                                "Debit minimal d'air neuf d'un local de travail en ventilation mecanique "
                                        + "(Code du travail, article R4222-6).",
                                WorkplaceRequest.class, service::workplace),
                        tools.of("cvc_gaines_catalog", "Catalogue",
                                "Materiaux et rugosites, types d'accessoires, series de dimensions normalisees, "
                                        + "categories de locaux de travail, air par defaut.",
                                Empty.class, empty -> service.catalog()))
                .build();
    }

    /** Entree vide (outil sans parametre). */
    record Empty() {
    }

    private static SchemaGenerator schemaGenerator() {
        SchemaGeneratorConfigBuilder config = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12,
                OptionPreset.PLAIN_JSON)
                .with(new JakartaValidationModule(JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED,
                        JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS))
                .with(new Swagger2Module())
                .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT);
        return new SchemaGenerator(config.build());
    }

    /** Fabrique d'outils : schema genere, validation, appel du service, resultat structure. */
    private record Tools(ObjectMapper objectMapper, Validator validator, McpJsonMapper jsonMapper,
                         SchemaGenerator schemas) {

        private static final McpSchema.ToolAnnotations READ_ONLY =
                new McpSchema.ToolAnnotations(null, true, false, true, false, null);

        <I> SyncToolSpecification of(String name, String title, String description, Class<I> input,
                                     Function<I, ?> call) {
            @SuppressWarnings("unchecked")
            Map<String, Object> schema = objectMapper.convertValue(schemas.generateSchema(input), Map.class);
            schema.remove("$schema");
            McpSchema.Tool tool = McpSchema.Tool.builder()
                    .name(name)
                    .title(title)
                    .description(description)
                    .inputSchema(schema)
                    .annotations(READ_ONLY)
                    .build();
            return new SyncToolSpecification(tool, (context, request) -> handle(name, input, call, request));
        }

        private <I> McpSchema.CallToolResult handle(String name, Class<I> input, Function<I, ?> call,
                                                    McpSchema.CallToolRequest request) {
            I args;
            try {
                args = objectMapper.convertValue(request.arguments() == null ? Map.of() : request.arguments(), input);
            } catch (IllegalArgumentException e) {
                return error("Arguments invalides : la structure ne correspond pas au schema de l'outil.");
            }
            Set<ConstraintViolation<I>> violations = validator.validate(args);
            if (!violations.isEmpty()) {
                return error("Arguments invalides : " + violations.stream()
                        .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                        .map(v -> v.getPropertyPath() + " " + v.getMessage())
                        .collect(Collectors.joining(", ")));
            }
            try {
                Object result = call.apply(args);
                return McpSchema.CallToolResult.builder()
                        .structuredContent(objectMapper.convertValue(result, Map.class))
                        .addTextContent(objectMapper.writeValueAsString(result))
                        .isError(false)
                        .build();
            } catch (IllegalArgumentException e) {
                // Erreur de saisie detectee par le moteur : message destine a l'utilisateur.
                return error(e.getMessage());
            } catch (Exception e) {
                log.error("Outil MCP {} : erreur inattendue", name, e);
                return error("Erreur interne lors du calcul.");
            }
        }

        private static McpSchema.CallToolResult error(String message) {
            return McpSchema.CallToolResult.builder().addTextContent(message).isError(true).build();
        }
    }
}
