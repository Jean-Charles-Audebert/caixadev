package dev.caixadev.tools.cvcgaines.api;

import dev.caixadev.tools.cvcgaines.api.CvcGainesDtos.*;
import dev.caixadev.tools.cvcgaines.engine.Regulatory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API machine de l'outil {@code cvc-gaines} v1 (ADR-0010), exposee sur
 * {@code https://api.caixadev.dev/cvc-gaines/v1}. Authentification par cle
 * d'API ({@code Authorization: Bearer cxd_...}, portee {@code cvc-gaines}),
 * portee par la chaine de securite dediee de {@code SecurityConfig}.
 *
 * <p>Calculs sans etat : aucune donnee n'est conservee. La logique est dans
 * {@link CvcGainesService}, partagee avec le serveur MCP.
 */
@RestController
@RequestMapping("/cvc-gaines/v1")
@Tag(name = "cvc-gaines", description = "Calculs aerauliques de reseaux de gaines")
@SecurityRequirement(name = "apiKey")
public class CvcGainesController {

    private final CvcGainesService service;

    public CvcGainesController(CvcGainesService service) {
        this.service = service;
    }

    @PostMapping("/sections/pressure-drop")
    @Operation(summary = "Perte de charge d'un troncon",
            description = "Vitesse, Reynolds, lambda (Colebrook-White), pertes lineique et singulieres.")
    public SectionResponse sectionPressureDrop(@Valid @RequestBody SectionRequest req) {
        return service.sectionPressureDrop(req);
    }

    @PostMapping("/sections/sizing")
    @Operation(summary = "Dimensionnement d'une gaine",
            description = "Plus petite dimension normalisee respectant la vitesse et la perte de charge lineique maximales.")
    public SizingResponse sizing(@Valid @RequestBody SizingRequest req) {
        return service.sizing(req);
    }

    @PostMapping("/networks/pressure-drop")
    @Operation(summary = "Calcul d'un reseau arborescent",
            description = "Debits sommes depuis les terminaux, perte de charge par chemin, chemin critique, "
                    + "desequilibres a compenser ; dimensionne les troncons sans section si sizing est fourni.")
    public NetworkResponse network(@Valid @RequestBody NetworkRequest req) {
        return service.network(req);
    }

    @PostMapping("/flows/dwelling")
    @Operation(summary = "Debits d'extraction d'un logement",
            description = "Arrete du 24 mars 1982 modifie : articles 3 (debits par piece) et 4 (debits reduits).")
    public Regulatory.DwellingResult dwelling(@Valid @RequestBody DwellingRequest req) {
        return service.dwelling(req);
    }

    @PostMapping("/flows/workplace")
    @Operation(summary = "Debit minimal d'air neuf d'un local de travail", description = "Code du travail, article R4222-6.")
    public WorkplaceResponse workplace(@Valid @RequestBody WorkplaceRequest req) {
        return service.workplace(req);
    }

    @GetMapping("/catalog")
    @Operation(summary = "Catalogue", description = "Materiaux et rugosites, types d'accessoires, series normalisees, "
            + "categories de locaux de travail, air par defaut.")
    public CatalogResponse catalog() {
        return service.catalog();
    }
}
