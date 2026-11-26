package dev.caixadev.tools.cvcgaines.engine;

/**
 * Section d'une gaine. Dimensions interieures, en m.
 *
 * <p>Pour une gaine rectangulaire, deux diametres coexistent :
 * <ul>
 *   <li>{@link #hydraulicDiameter()} {@code Dh = 4A/P = 2ab/(a+b)} : utilise
 *       avec la vitesse reelle pour le nombre de Reynolds et Darcy-Weisbach ;</li>
 *   <li>{@link Rectangular#equivalentDiameter()} (Huebscher)
 *       {@code De = 1,30 (ab)^0,625 / (a+b)^0,25} : diametre de la gaine
 *       circulaire de meme debit et meme perte de charge lineique (ASHRAE
 *       Handbook Fundamentals 2017, ch. 21, eq. 25). Donne a titre
 *       d'information, pour comparer avec une gaine circulaire.</li>
 * </ul>
 */
public sealed interface DuctShape {

    /** Section de passage, m2. */
    double area();

    /** Diametre hydraulique {@code 4A/P}, m. */
    double hydraulicDiameter();

    /** Vitesse moyenne pour un debit donne, m/s. */
    default double velocity(double flowM3s) {
        return flowM3s / area();
    }

    /** Gaine circulaire de diametre interieur {@code diameterM}. */
    record Circular(double diameterM) implements DuctShape {
        public Circular {
            Units.requirePositive(diameterM, "diametre");
        }

        @Override
        public double area() {
            return Math.PI * diameterM * diameterM / 4.0;
        }

        @Override
        public double hydraulicDiameter() {
            return diameterM;
        }
    }

    /** Gaine rectangulaire largeur x hauteur. */
    record Rectangular(double widthM, double heightM) implements DuctShape {
        public Rectangular {
            Units.requirePositive(widthM, "largeur");
            Units.requirePositive(heightM, "hauteur");
        }

        @Override
        public double area() {
            return widthM * heightM;
        }

        @Override
        public double hydraulicDiameter() {
            return 2 * widthM * heightM / (widthM + heightM);
        }

        /** Diametre equivalent de Huebscher, m. */
        public double equivalentDiameter() {
            return 1.30 * Math.pow(widthM * heightM, 0.625) / Math.pow(widthM + heightM, 0.25);
        }

        /** Rapport grand cote / petit cote (>= 1). */
        public double aspectRatio() {
            return Math.max(widthM, heightM) / Math.min(widthM, heightM);
        }
    }
}
