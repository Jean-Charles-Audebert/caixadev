package dev.caixadev.tools.cvcgaines.engine;

import java.util.List;

/**
 * Troncon de gaine a debit constant.
 *
 * @param flowM3s    debit volumique, m3/s
 * @param lengthM    longueur developpee, m (0 admis : accessoire seul)
 * @param shape      section
 * @param roughnessM rugosite absolue, m (voir {@link Material})
 * @param fittings   accessoires portes par le troncon (peut etre vide)
 */
public record Section(double flowM3s, double lengthM, DuctShape shape, double roughnessM, List<Fitting> fittings) {

    public Section {
        Units.requirePositive(flowM3s, "debit");
        Units.requireNonNegative(lengthM, "longueur");
        Units.requireNonNegative(roughnessM, "rugosite");
        if (shape == null) {
            throw new IllegalArgumentException("section de gaine obligatoire");
        }
        fittings = fittings == null ? List.of() : List.copyOf(fittings);
    }

    /** Calcule le troncon pour l'air donne. */
    public SectionResult calculate(Air air) {
        double velocity = shape.velocity(flowM3s);
        double dh = shape.hydraulicDiameter();
        double dynamic = air.dynamicPressure(velocity);
        double re = Friction.reynolds(velocity, dh, air);
        double lambda = Friction.frictionFactor(re, roughnessM, dh);
        double perMeter = Friction.pressureDropPerMeter(lambda, dh, dynamic);

        List<SectionResult.FittingResult> details = fittings.stream()
                .map(f -> new SectionResult.FittingResult(f.label(), f.pressureDrop(dynamic)))
                .toList();
        double singular = details.stream().mapToDouble(SectionResult.FittingResult::pressureDropPa).sum();
        double linear = perMeter * lengthM;

        return new SectionResult(velocity, dynamic, re, Friction.regime(re), lambda, perMeter,
                linear, singular, linear + singular, details);
    }
}
