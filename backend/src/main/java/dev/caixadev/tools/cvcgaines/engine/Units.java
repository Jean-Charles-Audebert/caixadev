package dev.caixadev.tools.cvcgaines.engine;

/** Conversions d'unites usuelles en aeraulique vers le SI, et controles. */
public final class Units {

    private Units() {
    }

    public static double m3hToM3s(double m3h) {
        return m3h / 3600.0;
    }

    public static double m3sToM3h(double m3s) {
        return m3s * 3600.0;
    }

    public static double mmToM(double mm) {
        return mm / 1000.0;
    }

    public static double mToMm(double m) {
        return m * 1000.0;
    }

    static double requirePositive(double value, String name) {
        if (!(value > 0) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(name + " doit etre strictement positif (recu : " + value + ")");
        }
        return value;
    }

    static double requireNonNegative(double value, String name) {
        if (!(value >= 0) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(name + " doit etre positif ou nul (recu : " + value + ")");
        }
        return value;
    }
}
