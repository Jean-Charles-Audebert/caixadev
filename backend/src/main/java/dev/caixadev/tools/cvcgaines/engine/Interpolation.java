package dev.caixadev.tools.cvcgaines.engine;

/** Interpolation lineaire dans une table ; hors bornes, valeur de la borne. */
final class Interpolation {

    private Interpolation() {
    }

    static double linear(double[] xs, double[] ys, double x) {
        if (x <= xs[0]) {
            return ys[0];
        }
        for (int i = 1; i < xs.length; i++) {
            if (x <= xs[i]) {
                double t = (x - xs[i - 1]) / (xs[i] - xs[i - 1]);
                return ys[i - 1] + t * (ys[i] - ys[i - 1]);
            }
        }
        return ys[ys.length - 1];
    }
}
