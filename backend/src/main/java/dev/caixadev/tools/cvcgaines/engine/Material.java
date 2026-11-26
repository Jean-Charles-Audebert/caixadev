package dev.caixadev.tools.cvcgaines.engine;

/**
 * Rugosites absolues usuelles des gaines.
 *
 * <p>Source : ASHRAE Handbook Fundamentals 2017, ch. 21 « Duct Design »,
 * tableau 1 (categories de rugosite). La valeur retenue est la valeur
 * representative de la categorie ; une rugosite saisie librement reste
 * possible ({@link Section} prend une rugosite en m).
 */
public enum Material {

    /** Acier galvanise spirale (agrafage helicoidal). ASHRAE « medium smooth ». */
    GALVANIZED_SPIRAL("Acier galvanise spirale", 0.09e-3),
    /** Acier galvanise a agrafage longitudinal, valeur usuelle en France. ASHRAE « average ». */
    GALVANIZED_LONGITUDINAL("Acier galvanise agrafe longitudinal", 0.15e-3),
    /** Aluminium. ASHRAE « smooth » (borne haute). */
    ALUMINIUM("Aluminium", 0.05e-3),
    /** Acier inoxydable. ASHRAE « smooth » (borne haute). */
    STAINLESS_STEEL("Acier inoxydable", 0.05e-3),
    /** PVC / PEHD. ASHRAE « smooth ». */
    PVC("PVC", 0.03e-3),
    /** Gaine flexible a armature metallique, totalement deployee. ASHRAE « medium rough ». */
    FLEXIBLE_EXTENDED("Gaine flexible deployee", 0.9e-3),
    /** Beton, maconnerie. ASHRAE « rough ». */
    CONCRETE("Beton", 3.0e-3);

    private final String label;
    private final double roughnessM;

    Material(String label, double roughnessM) {
        this.label = label;
        this.roughnessM = roughnessM;
    }

    public String label() {
        return label;
    }

    /** Rugosite absolue, m. */
    public double roughnessM() {
        return roughnessM;
    }
}
