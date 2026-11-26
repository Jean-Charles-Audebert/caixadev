package dev.caixadev.tools.cvcgaines.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.withinPercentage;

class AirAndFrictionTest {

    @Test
    void standardAirMatchesTabulatedValues() {
        // Air sec a 20 degC, 101325 Pa : rho 1,204 kg/m3, mu 1,813e-5 Pa.s.
        assertThat(Air.STANDARD.density()).isCloseTo(1.204, within(0.001));
        assertThat(Air.STANDARD.dynamicViscosity()).isCloseTo(1.813e-5, withinPercentage(0.5));
        assertThat(Air.STANDARD.kinematicViscosity()).isCloseTo(1.506e-5, withinPercentage(0.5));
    }

    @Test
    void densityDecreasesWithAltitudeAndTemperature() {
        // ISO 2533 : 1000 m -> 89 875 Pa.
        assertThat(new Air(20, 1000).pressurePa()).isCloseTo(89875, within(10.0));
        assertThat(new Air(20, 1000).density()).isLessThan(Air.STANDARD.density());
        assertThat(new Air(40, 0).density()).isCloseTo(1.127, within(0.002));
    }

    @Test
    void airRejectsOutOfRangeInput() {
        assertThatThrownBy(() -> new Air(200, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Air(20, 9000)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void laminarFrictionIs64OverRe() {
        assertThat(Friction.frictionFactor(1000, 0.15e-3, 0.2)).isEqualTo(0.064);
        assertThat(Friction.regime(1000)).isEqualTo(Friction.Regime.LAMINAR);
        assertThat(Friction.regime(3000)).isEqualTo(Friction.Regime.TRANSITIONAL);
        assertThat(Friction.regime(1e5)).isEqualTo(Friction.Regime.TURBULENT);
    }

    @ParameterizedTest
    @CsvSource({"1e4,0", "1e5,0.0001", "1.5e5,0.000476", "1e6,0.001", "1e7,0.01"})
    void colebrookSatisfiesItsEquationAndAgreesWithExplicitFormulas(double re, double relRough) {
        double lambda = Friction.colebrook(re, relRough);
        // Residu de l'equation implicite.
        double lhs = 1 / Math.sqrt(lambda);
        double rhs = -2 * Math.log10(relRough / 3.7 + 2.51 / (re * Math.sqrt(lambda)));
        assertThat(lhs).isCloseTo(rhs, within(1e-9));
        // Haaland et Swamee-Jain : ecart < 2 % en turbulent.
        assertThat(lambda).isCloseTo(Friction.haaland(re, relRough), withinPercentage(2));
        double swameeJain = 0.25 / Math.pow(Math.log10(relRough / 3.7 + 5.74 / Math.pow(re, 0.9)), 2);
        assertThat(lambda).isCloseTo(swameeJain, withinPercentage(2));
    }

    @Test
    void referenceCaseRound315At2000M3h() {
        // Cas d'ecole : O315, 2000 m3/h, galvanise agrafe (0,15 mm), air a 20 degC.
        // v = Q/A = 7,13 m/s ; Re ~ 1,49e5 ; dp/L ~ 1,85 Pa/m (abaques usuels : 1,8 a 1,9).
        Section s = new Section(Units.m3hToM3s(2000), 10, new DuctShape.Circular(0.315),
                Material.GALVANIZED_LONGITUDINAL.roughnessM(), List.of());
        SectionResult r = s.calculate(Air.STANDARD);

        assertThat(r.velocityMs()).isCloseTo(7.129, within(0.005));
        assertThat(r.reynolds()).isCloseTo(1.49e5, withinPercentage(1));
        assertThat(r.regime()).isEqualTo(Friction.Regime.TURBULENT);
        assertThat(r.linearPressureDropPaPerM()).isBetween(1.80, 1.90);
        assertThat(r.linearPressureDropPa()).isCloseTo(10 * r.linearPressureDropPaPerM(), within(1e-9));
        assertThat(r.totalPressureDropPa()).isEqualTo(r.linearPressureDropPa());
    }

    @Test
    void rectangularDuctUsesHydraulicDiameterAndReportsEquivalentDiameter() {
        DuctShape.Rectangular r = new DuctShape.Rectangular(0.4, 0.2);
        assertThat(r.hydraulicDiameter()).isCloseTo(0.2667, within(1e-4));
        // Huebscher, 400 x 200 : De = 305 mm (tables ASHRAE : 305).
        assertThat(r.equivalentDiameter()).isCloseTo(0.305, within(0.001));
        assertThat(r.aspectRatio()).isEqualTo(2.0);
    }

    @Test
    void sectionRejectsInvalidInput() {
        assertThatThrownBy(() -> new Section(0, 1, new DuctShape.Circular(0.2), 0, List.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("debit");
        assertThatThrownBy(() -> new DuctShape.Circular(-0.1))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("diametre");
    }
}
