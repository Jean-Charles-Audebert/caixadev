package dev.caixadev.tools.cvcgaines.api;

import dev.caixadev.tools.cvcgaines.api.CvcGainesDtos.*;
import dev.caixadev.tools.cvcgaines.engine.Air;
import dev.caixadev.tools.cvcgaines.engine.DuctShape;
import dev.caixadev.tools.cvcgaines.engine.Material;
import dev.caixadev.tools.cvcgaines.engine.Network;
import dev.caixadev.tools.cvcgaines.engine.Regulatory;
import dev.caixadev.tools.cvcgaines.engine.Section;
import dev.caixadev.tools.cvcgaines.engine.Sizing;
import dev.caixadev.tools.cvcgaines.engine.Units;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

import static dev.caixadev.tools.cvcgaines.api.CvcGainesMapper.round;

/**
 * API machine de l'outil {@code cvc-gaines} v1 (ADR-0010), exposee sur
 * {@code https://api.caixadev.dev/cvc-gaines/v1}. Authentification par cle
 * d'API ({@code Authorization: Bearer cxd_...}, portee {@code cvc-gaines}),
 * portee par la chaine de securite dediee de {@code SecurityConfig}.
 *
 * <p>Calculs sans etat : aucune donnee n'est conservee.
 */
@RestController
@RequestMapping("/cvc-gaines/v1")
@Tag(name = "cvc-gaines", description = "Calculs aerauliques de reseaux de gaines")
@SecurityRequirement(name = "apiKey")
public class CvcGainesController {

    @PostMapping("/sections/pressure-drop")
    @Operation(summary = "Perte de charge d'un troncon",
            description = "Vitesse, Reynolds, lambda (Colebrook-White), pertes lineique et singulieres.")
    public SectionResponse sectionPressureDrop(@Valid @RequestBody SectionRequest req) {
        DuctShape shape = CvcGainesMapper.shape(req.shape());
        Section section = new Section(Units.m3hToM3s(req.flowM3h()), req.lengthM(), shape,
                CvcGainesMapper.roughnessM(req.material(), req.roughnessMm()), CvcGainesMapper.fittings(req.fittings()));
        return CvcGainesMapper.section(section.calculate(CvcGainesMapper.air(req.air())), shape);
    }

    @PostMapping("/sections/sizing")
    @Operation(summary = "Dimensionnement d'une gaine",
            description = "Plus petite dimension normalisee respectant la vitesse et la perte de charge lineique maximales.")
    public SizingResponse sizing(@Valid @RequestBody SizingRequest req) {
        Sizing.Result r = Sizing.size(Units.m3hToM3s(req.flowM3h()),
                CvcGainesMapper.roughnessM(req.material(), req.roughnessMm()),
                CvcGainesMapper.criteria(req.criteria()), CvcGainesMapper.air(req.air()));
        return new SizingResponse(CvcGainesMapper.shape(r.shape()), round(r.velocityMs(), 3),
                round(r.pressureDropPaPerM(), 4), r.limitedBy());
    }

    @PostMapping("/networks/pressure-drop")
    @Operation(summary = "Calcul d'un reseau arborescent",
            description = "Debits sommes depuis les terminaux, perte de charge par chemin, chemin critique, "
                    + "desequilibres a compenser ; dimensionne les troncons sans section si sizing est fourni.")
    public NetworkResponse network(@Valid @RequestBody NetworkRequest req) {
        List<Network.Node> nodes = req.sections().stream()
                .map(s -> new Network.Node(s.id(), s.parentId(), s.lengthM(),
                        s.shape() == null ? null : CvcGainesMapper.shape(s.shape()),
                        CvcGainesMapper.roughnessM(s.material(), s.roughnessMm()),
                        CvcGainesMapper.fittings(s.fittings()),
                        s.terminalFlowM3h() == null ? null : Units.m3hToM3s(s.terminalFlowM3h())))
                .toList();
        Network.Result r = Network.calculate(nodes, CvcGainesMapper.air(req.air()),
                CvcGainesMapper.criteria(req.sizing()));

        List<NetworkSectionResultDto> sections = r.sections().stream()
                .map(n -> new NetworkSectionResultDto(n.id(), n.parentId(), round(Units.m3sToM3h(n.flowM3s()), 3),
                        CvcGainesMapper.shape(n.shape()), n.sizing() != null,
                        n.sizing() == null ? null : n.sizing().limitedBy(),
                        CvcGainesMapper.section(n.section(), n.shape())))
                .toList();
        List<PathResultDto> paths = r.paths().stream()
                .map(p -> new PathResultDto(p.terminalId(), p.path(), round(p.pressureDropPa(), 3),
                        round(p.imbalancePa(), 3)))
                .toList();
        return new NetworkResponse(round(Units.m3sToM3h(r.totalFlowM3s()), 3), r.criticalTerminalId(),
                round(r.criticalPressureDropPa(), 3), sections, paths);
    }

    @PostMapping("/flows/dwelling")
    @Operation(summary = "Debits d'extraction d'un logement",
            description = "Arrete du 24 mars 1982 modifie : articles 3 (debits par piece) et 4 (debits reduits).")
    public Regulatory.DwellingResult dwelling(@Valid @RequestBody DwellingRequest req) {
        return Regulatory.dwelling(new Regulatory.Dwelling(req.mainRooms(), req.bathrooms(),
                req.otherWaterRooms() == null ? 0 : req.otherWaterRooms(), req.toilets()));
    }

    @PostMapping("/flows/workplace")
    @Operation(summary = "Debit minimal d'air neuf d'un local de travail", description = "Code du travail, article R4222-6.")
    public WorkplaceResponse workplace(@Valid @RequestBody WorkplaceRequest req) {
        Regulatory.WorkplaceResult r = Regulatory.workplace(req.activity(), req.occupants());
        return new WorkplaceResponse(r.activity(), r.activity().label(), r.occupants(), r.flowPerOccupantM3h(),
                r.freshAirM3h(), "Code du travail, article R4222-6");
    }

    @GetMapping("/catalog")
    @Operation(summary = "Catalogue", description = "Materiaux et rugosites, types d'accessoires, series normalisees, "
            + "categories de locaux de travail, air par defaut.")
    public CatalogResponse catalog() {
        return new CatalogResponse(
                Arrays.stream(Material.values())
                        .map(m -> new MaterialDto(m, m.label(), round(Units.mToMm(m.roughnessM()), 4)))
                        .toList(),
                List.of(FittingType.values()),
                Sizing.CIRCULAR_DIAMETERS_MM,
                Sizing.RECTANGULAR_SIDES_MM,
                Arrays.stream(Regulatory.WorkplaceActivity.values())
                        .map(a -> new ActivityDto(a, a.label(), a.flowPerOccupantM3h()))
                        .toList(),
                new AirDto(Air.STANDARD.temperatureC(), Air.STANDARD.altitudeM()));
    }
}
