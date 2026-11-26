package dev.caixadev.tools.cvcgaines.engine;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class NetworkAndSizingTest {

    private static final double EPS = Material.GALVANIZED_SPIRAL.roughnessM();

    private static Network.Node node(String id, String parent, double length, double diameterM, Double flowM3h,
                                     Fitting... fittings) {
        return new Network.Node(id, parent, length, diameterM > 0 ? new DuctShape.Circular(diameterM) : null,
                EPS, List.of(fittings), flowM3h == null ? null : Units.m3hToM3s(flowM3h));
    }

    /**
     * Reseau en T : tronc A (O315, 10 m) -> B (O200, 5 m, 600 m3/h)
     *                                    -> C (O250, 12 m) -> D (O160, 3 m, 300 m3/h)
     *                                                      -> E (O200, 8 m, 500 m3/h)
     */
    private static List<Network.Node> sampleNetwork() {
        Fitting grille = new Fitting.FixedPressureDrop("Bouche", 15);
        return List.of(
                node("A", null, 10, 0.315, null, new Fitting.SharpEntry()),
                node("B", "A", 5, 0.200, 600.0, new Fitting.RoundElbow(1.0, 90), grille),
                node("C", "A", 12, 0.250, null),
                node("D", "C", 3, 0.160, 300.0, grille),
                node("E", "C", 8, 0.200, 500.0, new Fitting.RoundElbow(1.5, 90), grille));
    }

    @Test
    void flowsAreSummedFromTerminalsToFan() {
        Network.Result r = Network.calculate(sampleNetwork(), Air.STANDARD);
        Map<String, Network.NodeResult> byId = r.sections().stream()
                .collect(Collectors.toMap(Network.NodeResult::id, Function.identity()));

        assertThat(Units.m3sToM3h(r.totalFlowM3s())).isCloseTo(1400, within(1e-9));
        assertThat(Units.m3sToM3h(byId.get("C").flowM3s())).isCloseTo(800, within(1e-9));
        assertThat(r.sections()).extracting(Network.NodeResult::id).startsWith("A");
    }

    @Test
    void criticalPathIsTheLargestAndImbalancesAreRelativeToIt() {
        Network.Result r = Network.calculate(sampleNetwork(), Air.STANDARD);
        Map<String, Double> dp = r.sections().stream()
                .collect(Collectors.toMap(Network.NodeResult::id, n -> n.section().totalPressureDropPa()));

        Map<String, Network.PathResult> paths = r.paths().stream()
                .collect(Collectors.toMap(Network.PathResult::terminalId, Function.identity()));
        assertThat(paths.get("B").path()).containsExactly("A", "B");
        assertThat(paths.get("E").path()).containsExactly("A", "C", "E");
        assertThat(paths.get("B").pressureDropPa()).isCloseTo(dp.get("A") + dp.get("B"), within(1e-9));
        assertThat(paths.get("D").pressureDropPa()).isCloseTo(dp.get("A") + dp.get("C") + dp.get("D"), within(1e-9));

        double max = r.paths().stream().mapToDouble(Network.PathResult::pressureDropPa).max().orElseThrow();
        assertThat(r.criticalPressureDropPa()).isEqualTo(max);
        assertThat(paths.get(r.criticalTerminalId()).imbalancePa()).isZero();
        r.paths().forEach(p -> assertThat(p.imbalancePa())
                .isCloseTo(max - p.pressureDropPa(), within(1e-9)).isGreaterThanOrEqualTo(0));
    }

    @Test
    void symmetricNetworkIsBalanced() {
        Network.Result r = Network.calculate(List.of(
                node("A", null, 5, 0.250, null),
                node("B", "A", 4, 0.160, 250.0),
                node("C", "A", 4, 0.160, 250.0)), Air.STANDARD);
        assertThat(r.paths()).allSatisfy(p -> assertThat(p.imbalancePa()).isCloseTo(0, within(1e-12)));
    }

    @Test
    void invalidNetworksAreRejected() {
        assertThatThrownBy(() -> Network.calculate(List.of(
                node("A", null, 1, 0.2, 100.0), node("B", null, 1, 0.2, 100.0)), Air.STANDARD))
                .hasMessageContaining("plusieurs troncons racines");
        assertThatThrownBy(() -> Network.calculate(List.of(
                node("A", null, 1, 0.2, null), node("B", "X", 1, 0.2, 100.0)), Air.STANDARD))
                .hasMessageContaining("parent inconnu");
        assertThatThrownBy(() -> Network.calculate(List.of(
                node("A", null, 1, 0.2, null), node("B", "C", 1, 0.2, null), node("C", "B", 1, 0.2, 100.0)),
                Air.STANDARD))
                .hasMessageContaining("cycle");
        assertThatThrownBy(() -> Network.calculate(List.of(
                node("A", null, 1, 0.2, 100.0), node("B", "A", 1, 0.2, 100.0)), Air.STANDARD))
                .hasMessageContaining("ne se saisit que sur un terminal");
        assertThatThrownBy(() -> Network.calculate(List.of(
                node("A", null, 1, 0.2, null), node("B", "A", 1, 0.2, null)), Air.STANDARD))
                .hasMessageContaining("debit obligatoire");
        assertThatThrownBy(() -> Network.calculate(List.of(
                node("A", null, 1, 0.2, null), node("A", "A", 1, 0.2, 1.0)), Air.STANDARD))
                .hasMessageContaining("en double");
    }

    @Test
    void sizingPicksSmallestStandardDiameterMeetingBothCriteria() {
        // 2000 m3/h, v <= 7 m/s, dp/L <= 1,5 Pa/m : O315 donne 7,13 m/s -> O355 (5,6 m/s, ~1,0 Pa/m).
        Sizing.Result r = Sizing.size(Units.m3hToM3s(2000), Material.GALVANIZED_LONGITUDINAL.roughnessM(),
                Sizing.Criteria.circular(7, 1.5), Air.STANDARD);
        assertThat(r.shape()).isEqualTo(new DuctShape.Circular(0.355));
        assertThat(r.velocityMs()).isLessThanOrEqualTo(7);
        assertThat(r.pressureDropPaPerM()).isLessThanOrEqualTo(1.5);
        assertThat(r.limitedBy()).isEqualTo("vitesse maximale");

        // Critere de frottement plus severe : 0,8 Pa/m -> O400.
        Sizing.Result f = Sizing.size(Units.m3hToM3s(2000), Material.GALVANIZED_LONGITUDINAL.roughnessM(),
                Sizing.Criteria.circular(7, 0.8), Air.STANDARD);
        assertThat(f.shape()).isEqualTo(new DuctShape.Circular(0.400));
        assertThat(f.limitedBy()).isEqualTo("perte de charge lineique maximale");
    }

    @Test
    void rectangularSizingKeepsImposedHeight() {
        Sizing.Result r = Sizing.size(Units.m3hToM3s(3000), EPS, Sizing.Criteria.rectangular(5, 1.0, 0.3),
                Air.STANDARD);
        DuctShape.Rectangular shape = (DuctShape.Rectangular) r.shape();
        assertThat(shape.heightM()).isEqualTo(0.3);
        assertThat(Sizing.RECTANGULAR_SIDES_MM).contains((int) Math.round(Units.mToMm(shape.widthM())));
        assertThat(r.velocityMs()).isLessThanOrEqualTo(5);
    }

    @Test
    void sizingFailsBeyondTheStandardSeries() {
        assertThatThrownBy(() -> Sizing.size(Units.m3hToM3s(1_000_000), EPS, Sizing.Criteria.circular(5, 1),
                Air.STANDARD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aucune dimension normalisee");
        assertThatThrownBy(() -> new Sizing.Criteria(Sizing.ShapeKind.RECTANGULAR, 5, 1, null))
                .hasMessageContaining("hauteur imposee");
    }

    @Test
    void networkSizesSectionsWithoutShape() {
        Network.Result r = Network.calculate(List.of(
                node("A", null, 10, 0, null),
                node("B", "A", 5, 0, 600.0),
                node("C", "A", 5, 0.200, 400.0)), Air.STANDARD, Sizing.Criteria.circular(5, 1.0));
        Map<String, Network.NodeResult> byId = r.sections().stream()
                .collect(Collectors.toMap(Network.NodeResult::id, Function.identity()));
        assertThat(byId.get("A").sizing()).isNotNull();
        assertThat(byId.get("C").sizing()).isNull();
        assertThat(byId.get("C").shape()).isEqualTo(new DuctShape.Circular(0.2));
        assertThat(byId.get("A").section().velocityMs()).isLessThanOrEqualTo(5);

        assertThatThrownBy(() -> Network.calculate(List.of(node("A", null, 1, 0, 100.0)), Air.STANDARD))
                .hasMessageContaining("aucun critere de dimensionnement");
    }
}
