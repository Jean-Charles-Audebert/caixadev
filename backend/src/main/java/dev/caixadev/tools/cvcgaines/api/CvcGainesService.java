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
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

import static dev.caixadev.tools.cvcgaines.api.CvcGainesMapper.round;

/**
 * Cas d'usage de {@code cvc-gaines}, partages par l'API REST
 * ({@link CvcGainesController}) et le serveur MCP
 * ({@code dev.caixadev.tools.cvcgaines.mcp}) : memes entrees, memes
 * resultats, memes erreurs. Les entrees sont supposees deja validees
 * (Bean Validation) ; une incoherence detectee par le moteur leve
 * {@link IllegalArgumentException}.
 */
@Service
public class CvcGainesService {

    public SectionResponse sectionPressureDrop(SectionRequest req) {
        DuctShape shape = CvcGainesMapper.shape(req.shape());
        Section section = new Section(Units.m3hToM3s(req.flowM3h()), req.lengthM(), shape,
                CvcGainesMapper.roughnessM(req.material(), req.roughnessMm()), CvcGainesMapper.fittings(req.fittings()));
        return CvcGainesMapper.section(section.calculate(CvcGainesMapper.air(req.air())), shape);
    }

    public SizingResponse sizing(SizingRequest req) {
        Sizing.Result r = Sizing.size(Units.m3hToM3s(req.flowM3h()),
                CvcGainesMapper.roughnessM(req.material(), req.roughnessMm()),
                CvcGainesMapper.criteria(req.criteria()), CvcGainesMapper.air(req.air()));
        return new SizingResponse(CvcGainesMapper.shape(r.shape()), round(r.velocityMs(), 3),
                round(r.pressureDropPaPerM(), 4), r.limitedBy());
    }

    public NetworkResponse network(NetworkRequest req) {
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

    public Regulatory.DwellingResult dwelling(DwellingRequest req) {
        return Regulatory.dwelling(new Regulatory.Dwelling(req.mainRooms(), req.bathrooms(),
                req.otherWaterRooms() == null ? 0 : req.otherWaterRooms(), req.toilets()));
    }

    public WorkplaceResponse workplace(WorkplaceRequest req) {
        Regulatory.WorkplaceResult r = Regulatory.workplace(req.activity(), req.occupants());
        return new WorkplaceResponse(r.activity(), r.activity().label(), r.occupants(), r.flowPerOccupantM3h(),
                r.freshAirM3h(), "Code du travail, article R4222-6");
    }

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
