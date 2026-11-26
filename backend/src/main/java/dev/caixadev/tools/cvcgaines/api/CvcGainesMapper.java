package dev.caixadev.tools.cvcgaines.api;

import dev.caixadev.tools.cvcgaines.api.CvcGainesDtos.AirDto;
import dev.caixadev.tools.cvcgaines.api.CvcGainesDtos.FittingDto;
import dev.caixadev.tools.cvcgaines.api.CvcGainesDtos.FittingResultDto;
import dev.caixadev.tools.cvcgaines.api.CvcGainesDtos.SectionResponse;
import dev.caixadev.tools.cvcgaines.api.CvcGainesDtos.ShapeDto;
import dev.caixadev.tools.cvcgaines.api.CvcGainesDtos.ShapeKind;
import dev.caixadev.tools.cvcgaines.api.CvcGainesDtos.ShapeResultDto;
import dev.caixadev.tools.cvcgaines.api.CvcGainesDtos.SizingCriteriaDto;
import dev.caixadev.tools.cvcgaines.engine.Air;
import dev.caixadev.tools.cvcgaines.engine.DuctShape;
import dev.caixadev.tools.cvcgaines.engine.Fitting;
import dev.caixadev.tools.cvcgaines.engine.Material;
import dev.caixadev.tools.cvcgaines.engine.SectionResult;
import dev.caixadev.tools.cvcgaines.engine.Sizing;
import dev.caixadev.tools.cvcgaines.engine.Units;

import java.util.List;

/**
 * Traduction DTO (unites usuelles) <-> moteur (SI). Aucune formule ici :
 * uniquement des conversions et des valeurs par defaut documentees.
 */
final class CvcGainesMapper {

    /** Materiau par defaut : acier galvanise a agrafe longitudinal (usage courant en France). */
    static final Material DEFAULT_MATERIAL = Material.GALVANIZED_LONGITUDINAL;

    private CvcGainesMapper() {
    }

    static Air air(AirDto dto) {
        if (dto == null) {
            return Air.STANDARD;
        }
        return new Air(dto.temperatureC() == null ? 20.0 : dto.temperatureC(),
                dto.altitudeM() == null ? 0.0 : dto.altitudeM());
    }

    static double roughnessM(Material material, Double roughnessMm) {
        if (roughnessMm != null) {
            return Units.mmToM(roughnessMm);
        }
        return (material == null ? DEFAULT_MATERIAL : material).roughnessM();
    }

    static DuctShape shape(ShapeDto dto) {
        return switch (dto.kind()) {
            case CIRCULAR -> new DuctShape.Circular(Units.mmToM(require(dto.diameterMm(), "shape.diameterMm")));
            case RECTANGULAR -> new DuctShape.Rectangular(Units.mmToM(require(dto.widthMm(), "shape.widthMm")),
                    Units.mmToM(require(dto.heightMm(), "shape.heightMm")));
        };
    }

    static ShapeResultDto shape(DuctShape shape) {
        return switch (shape) {
            case DuctShape.Circular c -> new ShapeResultDto(ShapeKind.CIRCULAR, round(Units.mToMm(c.diameterM()), 1),
                    null, null);
            case DuctShape.Rectangular r -> new ShapeResultDto(ShapeKind.RECTANGULAR, null,
                    round(Units.mToMm(r.widthM()), 1), round(Units.mToMm(r.heightM()), 1));
        };
    }

    static List<Fitting> fittings(List<FittingDto> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream().map(CvcGainesMapper::fitting).toList();
    }

    static Fitting fitting(FittingDto f) {
        return switch (f.type()) {
            case ROUND_ELBOW -> new Fitting.RoundElbow(require(f.radiusRatio(), "fitting.radiusRatio"),
                    f.angleDeg() == null ? 90.0 : f.angleDeg());
            case MITERED_ROUND_ELBOW_90 -> new Fitting.MiteredRoundElbow90();
            case SUDDEN_EXPANSION -> new Fitting.SuddenExpansion(require(f.areaRatio(), "fitting.areaRatio"));
            case SUDDEN_CONTRACTION -> new Fitting.SuddenContraction(require(f.areaRatio(), "fitting.areaRatio"));
            case SHARP_ENTRY -> new Fitting.SharpEntry();
            case FREE_EXIT -> new Fitting.FreeExit();
            case CUSTOM_ZETA -> new Fitting.CustomZeta(f.label(), require(f.zeta(), "fitting.zeta"));
            case FIXED_PRESSURE_DROP -> new Fitting.FixedPressureDrop(f.label(),
                    require(f.pressureDropPa(), "fitting.pressureDropPa"));
        };
    }

    static Sizing.Criteria criteria(SizingCriteriaDto dto) {
        if (dto == null) {
            return null;
        }
        return new Sizing.Criteria(dto.kind(), dto.maxVelocityMs(), dto.maxPressureDropPaPerM(),
                dto.heightMm() == null ? null : Units.mmToM(dto.heightMm()));
    }

    static SectionResponse section(SectionResult r, DuctShape shape) {
        Double equivalent = shape instanceof DuctShape.Rectangular rect
                ? round(Units.mToMm(rect.equivalentDiameter()), 1)
                : null;
        return new SectionResponse(
                round(r.velocityMs(), 3),
                round(r.dynamicPressurePa(), 3),
                round(Units.mToMm(shape.hydraulicDiameter()), 1),
                equivalent,
                Math.round(r.reynolds()),
                r.regime(),
                round(r.frictionFactor(), 6),
                round(r.linearPressureDropPaPerM(), 4),
                round(r.linearPressureDropPa(), 3),
                round(r.singularPressureDropPa(), 3),
                round(r.totalPressureDropPa(), 3),
                r.fittings().stream().map(x -> new FittingResultDto(x.label(), round(x.pressureDropPa(), 3))).toList());
    }

    static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    private static double require(Double value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " est obligatoire pour ce type");
        }
        return value;
    }
}
