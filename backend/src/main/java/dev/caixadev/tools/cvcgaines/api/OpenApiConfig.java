package dev.caixadev.tools.cvcgaines.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contrat OpenAPI de {@code cvc-gaines} v1, publie sur
 * {@code /cvc-gaines/v1/openapi.json} (cle d'API requise). Base du futur
 * serveur MCP. Les routes de la plateforme ({@code /api/**}) n'y figurent pas
 * ({@code springdoc.paths-to-match}).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cvcGainesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("cvc-gaines")
                        .version("v1")
                        .description("Calculs aerauliques : pertes de charge (Darcy-Weisbach, Colebrook-White), "
                                + "reseaux arborescents et chemin critique, dimensionnement sur series normalisees, "
                                + "debits reglementaires (arrete du 24 mars 1982, Code du travail R4222-6). "
                                + "Unite dans chaque nom de champ."))
                .addServersItem(new Server().url("https://api.caixadev.dev"))
                .components(new Components().addSecuritySchemes("apiKey", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .description("Cle d'API cxd_... (portee cvc-gaines), creee par le superadmin.")));
    }
}
