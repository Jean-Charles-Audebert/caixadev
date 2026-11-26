package dev.caixadev.tools.cvcgaines.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Debits reglementaires d'air (France).
 *
 * <h2>Logements — arrete du 24 mars 1982 modifie (arrete du 28 octobre 1983)</h2>
 * <ul>
 *   <li>Article 3 : debits extraits que l'installation doit pouvoir atteindre,
 *       simultanement ou non, par piece de service, selon le nombre de pieces
 *       principales. Une piece sans cloison entre sejour et chambre compte pour
 *       deux pieces principales. Un cabinet d'aisances est « multiple » des
 *       qu'il y en a au moins deux dans le logement, meme si l'un est dans une
 *       salle d'eau. En logement d'une piece, une bouche commune salle de bains
 *       + WC attenants est admise, a 15 m3/h.</li>
 *   <li>Article 4 : debits reduits admis par des dispositifs individuels de
 *       reglage — debit total minimal et debit minimal en cuisine.</li>
 * </ul>
 * Source lue : Legifrance, JORFTEXT000000862344 (version consolidee).
 * <b>A RELIRE (porteur)</b> : colonnes « cabinet d'aisances unique / multiple »
 * de l'article 3, non confirmees cellule par cellule sur la version
 * numerique (tableau mis a plat) ; valeurs ci-dessous selon le tableau
 * couramment reproduit.
 *
 * <h2>Lieux de travail — Code du travail, article R4222-6</h2>
 * Debit minimal d'air neuf par occupant en ventilation mecanique, locaux a
 * pollution non specifique.
 */
public final class Regulatory {

    private Regulatory() {
    }

    // Article 3, lignes : 1, 2, 3, 4, 5 et plus pieces principales (m3/h).
    private static final int[] KITCHEN = {75, 90, 105, 120, 135};
    private static final int[] BATHROOM = {15, 15, 30, 30, 30};
    private static final int[] OTHER_WATER_ROOM = {15, 15, 15, 15, 15};
    private static final int[] WC_SINGLE = {15, 15, 15, 30, 30};
    private static final int[] WC_MULTIPLE = {15, 15, 15, 15, 15};

    // Article 4, colonnes : 1 a 7 et plus pieces principales (m3/h).
    private static final int[] MIN_TOTAL = {35, 60, 75, 90, 105, 120, 135};
    private static final int[] MIN_KITCHEN = {20, 30, 45, 45, 45, 45, 45};

    /**
     * @param mainRooms       nombre de pieces principales (>= 1)
     * @param bathrooms       salles de bains ou de douches (la premiere compte
     *                        en « salle de bains », les suivantes en « autre salle d'eau »)
     * @param otherWaterRooms autres salles d'eau (salle d'eau secondaire, buanderie...)
     * @param toilets         cabinets d'aisances (0 a n)
     */
    public record Dwelling(int mainRooms, int bathrooms, int otherWaterRooms, int toilets) {
        public Dwelling {
            if (mainRooms < 1 || mainRooms > 20) {
                throw new IllegalArgumentException("nombre de pieces principales hors domaine [1 ; 20] : " + mainRooms);
            }
            if (bathrooms < 0 || otherWaterRooms < 0 || toilets < 0) {
                throw new IllegalArgumentException("les nombres de pieces de service doivent etre positifs ou nuls");
            }
        }
    }

    public record RoomFlow(String room, int flowM3h) {
    }

    /**
     * @param rooms               debits extraits par piece de service (article 3), m3/h
     * @param totalExtractM3h     somme des debits de l'article 3, m3/h
     * @param minimumTotalM3h     debit total minimal en debit reduit (article 4), m3/h
     * @param minimumKitchenM3h   debit minimal en cuisine en debit reduit (article 4), m3/h
     * @param notes               regles appliquees
     */
    public record DwellingResult(List<RoomFlow> rooms, int totalExtractM3h, int minimumTotalM3h,
                                 int minimumKitchenM3h, List<String> notes) {
    }

    public static DwellingResult dwelling(Dwelling d) {
        int row = Math.min(d.mainRooms(), 5) - 1;
        List<RoomFlow> rooms = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        rooms.add(new RoomFlow("Cuisine", KITCHEN[row]));
        for (int i = 0; i < d.bathrooms(); i++) {
            rooms.add(i == 0
                    ? new RoomFlow("Salle de bains", BATHROOM[row])
                    : new RoomFlow("Autre salle d'eau", OTHER_WATER_ROOM[row]));
        }
        for (int i = 0; i < d.otherWaterRooms(); i++) {
            rooms.add(new RoomFlow("Autre salle d'eau", OTHER_WATER_ROOM[row]));
        }
        boolean multiple = d.toilets() >= 2;
        for (int i = 0; i < d.toilets(); i++) {
            rooms.add(new RoomFlow(multiple ? "WC (multiples)" : "WC (unique)",
                    multiple ? WC_MULTIPLE[row] : WC_SINGLE[row]));
        }
        if (multiple) {
            notes.add("Au moins deux WC : valeurs « cabinets d'aisances multiples » (art. 3).");
        }
        if (d.mainRooms() >= 5) {
            notes.add("5 pieces principales et plus : ligne « 5 et plus » de l'article 3.");
        }
        notes.add("Une piece sans cloison entre sejour et chambre compte pour deux pieces principales (art. 3).");
        int total = rooms.stream().mapToInt(RoomFlow::flowM3h).sum();
        int col = Math.min(d.mainRooms(), 7) - 1;
        return new DwellingResult(List.copyOf(rooms), total, MIN_TOTAL[col], MIN_KITCHEN[col], List.copyOf(notes));
    }

    /** Categories de locaux de l'article R4222-6 du Code du travail. */
    public enum WorkplaceActivity {
        OFFICE("Bureaux, locaux sans travail physique", 25),
        CATERING_SALES_MEETING("Locaux de restauration, de vente, de reunion", 30),
        LIGHT_PHYSICAL_WORK("Ateliers et locaux avec travail physique leger", 45),
        OTHER_WORKSHOP("Autres ateliers et locaux", 60);

        private final String label;
        private final int flowPerOccupantM3h;

        WorkplaceActivity(String label, int flowPerOccupantM3h) {
            this.label = label;
            this.flowPerOccupantM3h = flowPerOccupantM3h;
        }

        public String label() {
            return label;
        }

        public int flowPerOccupantM3h() {
            return flowPerOccupantM3h;
        }
    }

    public record WorkplaceResult(WorkplaceActivity activity, int occupants, int flowPerOccupantM3h,
                                  int freshAirM3h) {
    }

    /** Debit minimal d'air neuf d'un local de travail (R4222-6). */
    public static WorkplaceResult workplace(WorkplaceActivity activity, int occupants) {
        if (activity == null) {
            throw new IllegalArgumentException("categorie de local obligatoire");
        }
        if (occupants < 1 || occupants > 100_000) {
            throw new IllegalArgumentException("nombre d'occupants hors domaine [1 ; 100000] : " + occupants);
        }
        return new WorkplaceResult(activity, occupants, activity.flowPerOccupantM3h(),
                activity.flowPerOccupantM3h() * occupants);
    }
}
