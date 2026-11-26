package dev.caixadev.tools.cvcgaines.engine;

import java.util.List;

/**
 * Resultat du calcul d'un troncon.
 *
 * @param velocityMs           vitesse moyenne, m/s
 * @param dynamicPressurePa    pression dynamique, Pa
 * @param reynolds             nombre de Reynolds (sur le diametre hydraulique)
 * @param regime               regime d'ecoulement
 * @param frictionFactor       coefficient de Darcy lambda
 * @param linearPressureDropPaPerM perte de charge lineique, Pa/m
 * @param linearPressureDropPa perte de charge lineique x longueur, Pa
 * @param singularPressureDropPa somme des pertes singulieres, Pa
 * @param totalPressureDropPa  perte de charge totale du troncon, Pa
 * @param fittings             detail par accessoire
 */
public record SectionResult(
        double velocityMs,
        double dynamicPressurePa,
        double reynolds,
        Friction.Regime regime,
        double frictionFactor,
        double linearPressureDropPaPerM,
        double linearPressureDropPa,
        double singularPressureDropPa,
        double totalPressureDropPa,
        List<FittingResult> fittings) {

    public record FittingResult(String label, double pressureDropPa) {
    }
}
