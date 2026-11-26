package dev.caixadev.tools.cvcgaines.engine;

import java.util.List;

/**
 * Dimensionnement d'une gaine par la methode a perte de charge lineique
 * constante, bornee en vitesse : on retient la <b>plus petite dimension
 * normalisee</b> qui respecte a la fois {@code v <= vMax} et
 * {@code dp/L <= (dp/L)max}.
 *
 * <p>Series normalisees (dimensions interieures nominales, mm) :
 * <ul>
 *   <li>circulaire : diametres de la serie usuelle NF EN 1506 ;</li>
 *   <li>rectangulaire : cotes de la serie NF EN 1505 ; la hauteur est imposee
 *       (encombrement), la largeur est choisie dans la serie.</li>
 * </ul>
 */
public final class Sizing {

    /** Diametres usuels, mm (NF EN 1506). */
    public static final List<Integer> CIRCULAR_DIAMETERS_MM = List.of(
            63, 80, 100, 125, 160, 200, 250, 315, 355, 400, 450, 500, 560, 630, 710, 800, 900,
            1000, 1120, 1250);

    /** Cotes usuelles, mm (NF EN 1505). */
    public static final List<Integer> RECTANGULAR_SIDES_MM = List.of(
            100, 150, 200, 250, 300, 400, 500, 600, 800, 1000, 1200, 1400, 1600, 1800, 2000);

    private Sizing() {
    }

    public enum ShapeKind { CIRCULAR, RECTANGULAR }

    /**
     * @param kind                   forme recherchee
     * @param maxVelocityMs          vitesse maximale, m/s
     * @param maxPressureDropPaPerM  perte de charge lineique maximale, Pa/m
     * @param heightM                hauteur imposee (rectangulaire uniquement), m
     */
    public record Criteria(ShapeKind kind, double maxVelocityMs, double maxPressureDropPaPerM, Double heightM) {
        public Criteria {
            if (kind == null) {
                throw new IllegalArgumentException("forme a dimensionner obligatoire");
            }
            Units.requirePositive(maxVelocityMs, "vitesse maximale");
            Units.requirePositive(maxPressureDropPaPerM, "perte de charge lineique maximale");
            if (kind == ShapeKind.RECTANGULAR) {
                if (heightM == null) {
                    throw new IllegalArgumentException("hauteur imposee obligatoire pour une gaine rectangulaire");
                }
                Units.requirePositive(heightM, "hauteur");
            }
        }

        public static Criteria circular(double maxVelocityMs, double maxPressureDropPaPerM) {
            return new Criteria(ShapeKind.CIRCULAR, maxVelocityMs, maxPressureDropPaPerM, null);
        }

        public static Criteria rectangular(double maxVelocityMs, double maxPressureDropPaPerM, double heightM) {
            return new Criteria(ShapeKind.RECTANGULAR, maxVelocityMs, maxPressureDropPaPerM, heightM);
        }
    }

    /**
     * @param shape            dimension retenue
     * @param velocityMs       vitesse obtenue, m/s
     * @param pressureDropPaPerM perte de charge lineique obtenue, Pa/m
     * @param limitedBy        critere determinant (celui qui a ecarte la dimension precedente)
     */
    public record Result(DuctShape shape, double velocityMs, double pressureDropPaPerM, String limitedBy) {
    }

    /** Dimensionne une gaine pour un debit donne. */
    public static Result size(double flowM3s, double roughnessM, Criteria criteria, Air air) {
        Units.requirePositive(flowM3s, "debit");
        String limitedBy = "dimension minimale de la serie";
        for (DuctShape candidate : candidates(criteria)) {
            SectionResult r = new Section(flowM3s, 1.0, candidate, roughnessM, List.of()).calculate(air);
            boolean velocityOk = r.velocityMs() <= criteria.maxVelocityMs();
            boolean frictionOk = r.linearPressureDropPaPerM() <= criteria.maxPressureDropPaPerM();
            if (velocityOk && frictionOk) {
                return new Result(candidate, r.velocityMs(), r.linearPressureDropPaPerM(), limitedBy);
            }
            limitedBy = !velocityOk ? "vitesse maximale" : "perte de charge lineique maximale";
        }
        throw new IllegalArgumentException("aucune dimension normalisee ne respecte les criteres pour "
                + Math.round(Units.m3sToM3h(flowM3s)) + " m3/h (critere limitant : " + limitedBy + ")");
    }

    private static List<DuctShape> candidates(Criteria criteria) {
        if (criteria.kind() == ShapeKind.CIRCULAR) {
            return CIRCULAR_DIAMETERS_MM.stream()
                    .<DuctShape>map(d -> new DuctShape.Circular(Units.mmToM(d)))
                    .toList();
        }
        return RECTANGULAR_SIDES_MM.stream()
                .<DuctShape>map(w -> new DuctShape.Rectangular(Units.mmToM(w), criteria.heightM()))
                .toList();
    }
}
