package dev.caixadev.tools.cvcgaines.engine;

/**
 * Coefficient de frottement et perte de charge lineique (Darcy-Weisbach).
 *
 * <ul>
 *   <li>Laminaire (Re &lt; 2300) : {@code lambda = 64 / Re}.</li>
 *   <li>Sinon : Colebrook-White
 *       {@code 1/sqrt(lambda) = -2 log10( eps/(3,7 Dh) + 2,51/(Re sqrt(lambda)) )},
 *       resolu par point fixe a partir de l'approximation de Haaland.
 *       La zone de transition (2300 &lt;= Re &lt; 4000) est signalee : Colebrook y
 *       donne une valeur prudente, sans garantie d'exactitude.</li>
 *   <li>{@code dp/L = lambda / Dh * rho v^2 / 2}.</li>
 * </ul>
 */
public final class Friction {

    public static final double LAMINAR_LIMIT = 2300;
    public static final double TURBULENT_LIMIT = 4000;
    private static final double TOLERANCE = 1e-12;
    private static final int MAX_ITERATIONS = 100;

    private Friction() {
    }

    public enum Regime { LAMINAR, TRANSITIONAL, TURBULENT }

    public static double reynolds(double velocityMs, double hydraulicDiameterM, Air air) {
        return velocityMs * hydraulicDiameterM / air.kinematicViscosity();
    }

    public static Regime regime(double reynolds) {
        if (reynolds < LAMINAR_LIMIT) {
            return Regime.LAMINAR;
        }
        return reynolds < TURBULENT_LIMIT ? Regime.TRANSITIONAL : Regime.TURBULENT;
    }

    /** Coefficient de frottement de Darcy. */
    public static double frictionFactor(double reynolds, double roughnessM, double hydraulicDiameterM) {
        Units.requirePositive(reynolds, "nombre de Reynolds");
        Units.requireNonNegative(roughnessM, "rugosite");
        Units.requirePositive(hydraulicDiameterM, "diametre hydraulique");
        if (reynolds < LAMINAR_LIMIT) {
            return 64.0 / reynolds;
        }
        return colebrook(reynolds, roughnessM / hydraulicDiameterM);
    }

    /** Colebrook-White, rugosite relative {@code eps/Dh}. */
    static double colebrook(double reynolds, double relativeRoughness) {
        double x = 1.0 / Math.sqrt(haaland(reynolds, relativeRoughness));
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            double next = -2.0 * Math.log10(relativeRoughness / 3.7 + 2.51 * x / reynolds);
            if (Math.abs(next - x) < TOLERANCE) {
                return 1.0 / (next * next);
            }
            x = next;
        }
        throw new IllegalStateException("Colebrook-White n'a pas converge (Re=" + reynolds + ")");
    }

    /** Approximation explicite de Haaland (1983), ecart &lt; 2 % avec Colebrook. */
    static double haaland(double reynolds, double relativeRoughness) {
        double x = -1.8 * Math.log10(Math.pow(relativeRoughness / 3.7, 1.11) + 6.9 / reynolds);
        return 1.0 / (x * x);
    }

    /** Perte de charge lineique, Pa/m. */
    public static double pressureDropPerMeter(double frictionFactor, double hydraulicDiameterM,
                                              double dynamicPressurePa) {
        return frictionFactor / hydraulicDiameterM * dynamicPressurePa;
    }
}
