package dev.caixadev.tools.cvcgaines.engine;

/**
 * Perte de charge singuliere portee par un troncon.
 *
 * <p>Convention : un coefficient zeta est <b>rapporte a la pression
 * dynamique du troncon qui porte l'accessoire</b> ({@code dp = zeta . rho v^2 / 2}).
 * <ul>
 *   <li>Un elargissement se place sur le troncon amont (le plus petit),
 *       un retrecissement sur le troncon aval (le plus petit) : dans les deux
 *       cas, la vitesse de reference est la plus grande.</li>
 *   <li>Les tes et derivations, dont le zeta depend des rapports de debit et
 *       de section, ne sont pas modelises en v1 : saisir un
 *       {@link CustomZeta} tire d'une base de donnees de raccords.</li>
 *   <li>Les bouches, grilles, registres, filtres et batteries se saisissent par
 *       leur perte de charge fabricant ({@link FixedPressureDrop}).</li>
 * </ul>
 */
public sealed interface Fitting {

    /** Libelle lisible, repris dans les resultats. */
    String label();

    /** Perte de charge de l'accessoire pour la pression dynamique du troncon, Pa. */
    double pressureDrop(double dynamicPressurePa);

    /**
     * Coude circulaire a rayon de courbure lisse (embouti).
     *
     * <p>ASHRAE Duct Fitting Database, raccord CD3-1 : zeta a 90 degres selon
     * r/D, multiplie par le facteur d'angle K (meme raccord). Interpolation
     * lineaire entre les points tabules ; hors table, valeur de la borne.
     *
     * @param radiusRatio rapport rayon de courbure / diametre (0,5 a 2,5)
     * @param angleDeg    angle de deviation (0 a 180 degres)
     */
    record RoundElbow(double radiusRatio, double angleDeg) implements Fitting {

        private static final double[] R_D = {0.5, 0.75, 1.0, 1.5, 2.0, 2.5};
        private static final double[] ZETA_90 = {0.71, 0.33, 0.22, 0.15, 0.13, 0.12};
        private static final double[] ANGLE = {0, 20, 30, 45, 60, 75, 90, 110, 130, 150, 180};
        private static final double[] K = {0, 0.31, 0.45, 0.60, 0.78, 0.90, 1.00, 1.13, 1.20, 1.28, 1.40};

        public RoundElbow {
            if (!(radiusRatio >= 0.5 && radiusRatio <= 2.5)) {
                throw new IllegalArgumentException("r/D hors domaine [0,5 ; 2,5] : " + radiusRatio);
            }
            if (!(angleDeg > 0 && angleDeg <= 180)) {
                throw new IllegalArgumentException("angle hors domaine ]0 ; 180] degres : " + angleDeg);
            }
        }

        public double zeta() {
            return Interpolation.linear(R_D, ZETA_90, radiusRatio) * Interpolation.linear(ANGLE, K, angleDeg);
        }

        @Override
        public String label() {
            return "Coude circulaire r/D=" + radiusRatio + " " + angleDeg + " deg";
        }

        @Override
        public double pressureDrop(double dynamicPressurePa) {
            return zeta() * dynamicPressurePa;
        }
    }

    /**
     * Coude circulaire a angle vif (onglet unique, sans aubes), 90 degres.
     * Valeur usuelle zeta = 1,2 (ASHRAE Duct Fitting Database, coude circulaire
     * a onglet unique sans aubes ; Idelchik, diagramme 6-5, ordre de grandeur
     * 1,1 a 1,3 selon Re et la rugosite).
     */
    record MiteredRoundElbow90() implements Fitting {
        public static final double ZETA = 1.2;

        @Override
        public String label() {
            return "Coude circulaire a angle vif 90 deg";
        }

        @Override
        public double pressureDrop(double dynamicPressurePa) {
            return ZETA * dynamicPressurePa;
        }
    }

    /**
     * Elargissement brusque (Borda-Carnot), {@code zeta = (1 - A1/A2)^2},
     * rapporte a la vitesse amont. Idelchik, Handbook of Hydraulic
     * Resistance, diagramme 4-1.
     *
     * @param areaRatio A1/A2, section du troncon / section aval (0 a 1)
     */
    record SuddenExpansion(double areaRatio) implements Fitting {
        public SuddenExpansion {
            if (!(areaRatio > 0 && areaRatio <= 1)) {
                throw new IllegalArgumentException("A1/A2 hors domaine ]0 ; 1] : " + areaRatio);
            }
        }

        public double zeta() {
            return (1 - areaRatio) * (1 - areaRatio);
        }

        @Override
        public String label() {
            return "Elargissement brusque A1/A2=" + areaRatio;
        }

        @Override
        public double pressureDrop(double dynamicPressurePa) {
            return zeta() * dynamicPressurePa;
        }
    }

    /**
     * Retrecissement brusque, {@code zeta = 0,5 (1 - A2/A1)^0,75}, rapporte a la
     * vitesse aval. Idelchik, diagramme 4-9 (approximation usuelle).
     *
     * @param areaRatio A2/A1, section du troncon / section amont (0 a 1)
     */
    record SuddenContraction(double areaRatio) implements Fitting {
        public SuddenContraction {
            if (!(areaRatio > 0 && areaRatio <= 1)) {
                throw new IllegalArgumentException("A2/A1 hors domaine ]0 ; 1] : " + areaRatio);
            }
        }

        public double zeta() {
            return 0.5 * Math.pow(1 - areaRatio, 0.75);
        }

        @Override
        public String label() {
            return "Retrecissement brusque A2/A1=" + areaRatio;
        }

        @Override
        public double pressureDrop(double dynamicPressurePa) {
            return zeta() * dynamicPressurePa;
        }
    }

    /** Entree de gaine a bord vif, affleurante depuis un plenum : zeta = 0,5 (Idelchik, diagramme 3-1). */
    record SharpEntry() implements Fitting {
        public static final double ZETA = 0.5;

        @Override
        public String label() {
            return "Entree a bord vif";
        }

        @Override
        public double pressureDrop(double dynamicPressurePa) {
            return ZETA * dynamicPressurePa;
        }
    }

    /** Sortie libre vers un plenum ou l'ambiance : toute la pression dynamique est perdue, zeta = 1. */
    record FreeExit() implements Fitting {
        public static final double ZETA = 1.0;

        @Override
        public String label() {
            return "Sortie libre";
        }

        @Override
        public double pressureDrop(double dynamicPressurePa) {
            return ZETA * dynamicPressurePa;
        }
    }

    /** Coefficient saisi par l'utilisateur (tes, derivations, raccords speciaux). */
    record CustomZeta(String label, double zeta) implements Fitting {
        public CustomZeta {
            if (label == null || label.isBlank()) {
                throw new IllegalArgumentException("libelle obligatoire pour un zeta personnalise");
            }
            Units.requireNonNegative(zeta, "zeta");
        }

        @Override
        public double pressureDrop(double dynamicPressurePa) {
            return zeta * dynamicPressurePa;
        }
    }

    /** Perte de charge fixe, donnee fabricant (bouche, grille, registre, filtre). */
    record FixedPressureDrop(String label, double pressureDropPa) implements Fitting {
        public FixedPressureDrop {
            if (label == null || label.isBlank()) {
                throw new IllegalArgumentException("libelle obligatoire pour une perte de charge fixe");
            }
            Units.requireNonNegative(pressureDropPa, "perte de charge");
        }

        @Override
        public double pressureDrop(double dynamicPressurePa) {
            return pressureDropPa;
        }
    }
}
