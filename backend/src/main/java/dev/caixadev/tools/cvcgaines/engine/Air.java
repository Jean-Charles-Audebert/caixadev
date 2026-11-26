package dev.caixadev.tools.cvcgaines.engine;

/**
 * Proprietes de l'air sec a une temperature et une altitude donnees.
 *
 * <ul>
 *   <li>Pression : atmosphere type ISO 2533 (troposphere),
 *       {@code p = 101325 (1 - 2,25577e-5 h)^5,25588}.</li>
 *   <li>Masse volumique : gaz parfait, {@code rho = p / (R T)},
 *       R = 287,058 J/(kg.K) pour l'air sec.</li>
 *   <li>Viscosite dynamique : loi de Sutherland,
 *       mu0 = 1,716e-5 Pa.s a T0 = 273,15 K, S = 110,4 K.</li>
 * </ul>
 * A 20 degC et 0 m : rho = 1,204 kg/m3, mu = 1,813e-5 Pa.s, nu = 1,506e-5 m2/s.
 *
 * @param temperatureC temperature de l'air, degC (entre -50 et 150)
 * @param altitudeM    altitude du site, m (entre -500 et 5000)
 */
public record Air(double temperatureC, double altitudeM) {

    static final double R_DRY_AIR = 287.058;
    private static final double MU0 = 1.716e-5;
    private static final double T0 = 273.15;
    private static final double SUTHERLAND = 110.4;

    /** Air de reference : 20 degC au niveau de la mer. */
    public static final Air STANDARD = new Air(20.0, 0.0);

    public Air {
        if (!(temperatureC >= -50 && temperatureC <= 150)) {
            throw new IllegalArgumentException("temperature hors domaine [-50 ; 150] degC : " + temperatureC);
        }
        if (!(altitudeM >= -500 && altitudeM <= 5000)) {
            throw new IllegalArgumentException("altitude hors domaine [-500 ; 5000] m : " + altitudeM);
        }
    }

    public double temperatureK() {
        return temperatureC + T0;
    }

    /** Pression atmospherique, Pa. */
    public double pressurePa() {
        return 101325.0 * Math.pow(1 - 2.25577e-5 * altitudeM, 5.25588);
    }

    /** Masse volumique, kg/m3. */
    public double density() {
        return pressurePa() / (R_DRY_AIR * temperatureK());
    }

    /** Viscosite dynamique, Pa.s. */
    public double dynamicViscosity() {
        double t = temperatureK();
        return MU0 * Math.pow(t / T0, 1.5) * (T0 + SUTHERLAND) / (t + SUTHERLAND);
    }

    /** Viscosite cinematique, m2/s. */
    public double kinematicViscosity() {
        return dynamicViscosity() / density();
    }

    /** Pression dynamique {@code rho v^2 / 2}, Pa. */
    public double dynamicPressure(double velocityMs) {
        return density() * velocityMs * velocityMs / 2.0;
    }
}
