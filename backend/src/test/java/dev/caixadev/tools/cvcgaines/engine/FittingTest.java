package dev.caixadev.tools.cvcgaines.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class FittingTest {

    @Test
    void roundElbowUsesTableAndAngleFactor() {
        assertThat(new Fitting.RoundElbow(1.0, 90).zeta()).isCloseTo(0.22, within(1e-12));
        assertThat(new Fitting.RoundElbow(1.5, 90).zeta()).isCloseTo(0.15, within(1e-12));
        // 45 degres : K = 0,60.
        assertThat(new Fitting.RoundElbow(1.0, 45).zeta()).isCloseTo(0.132, within(1e-12));
        // Interpolation entre r/D 1,0 (0,22) et 1,5 (0,15).
        assertThat(new Fitting.RoundElbow(1.25, 90).zeta()).isCloseTo(0.185, within(1e-12));
        assertThatThrownBy(() -> new Fitting.RoundElbow(3.0, 90)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bordaCarnotLimits() {
        assertThat(new Fitting.SuddenExpansion(1.0).zeta()).isZero();
        assertThat(new Fitting.SuddenExpansion(0.5).zeta()).isCloseTo(0.25, within(1e-12));
        assertThat(new Fitting.SuddenContraction(1.0).zeta()).isZero();
        assertThat(new Fitting.SuddenContraction(0.5).zeta()).isCloseTo(0.5 * Math.pow(0.5, 0.75), within(1e-12));
    }

    @Test
    void fittingsAddToSectionSingularLoss() {
        Section s = new Section(Units.m3hToM3s(1000), 5, new DuctShape.Circular(0.25),
                Material.GALVANIZED_SPIRAL.roughnessM(),
                List.of(new Fitting.RoundElbow(1.0, 90), new Fitting.FreeExit(),
                        new Fitting.FixedPressureDrop("Bouche de soufflage", 20)));
        SectionResult r = s.calculate(Air.STANDARD);

        double pd = r.dynamicPressurePa();
        assertThat(r.singularPressureDropPa()).isCloseTo((0.22 + 1.0) * pd + 20, within(1e-9));
        assertThat(r.fittings()).hasSize(3);
        assertThat(r.totalPressureDropPa()).isCloseTo(r.linearPressureDropPa() + r.singularPressureDropPa(), within(1e-9));
    }

    @Test
    void customFittingsRequireLabelAndNonNegativeValue() {
        assertThatThrownBy(() -> new Fitting.CustomZeta(" ", 0.3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Fitting.CustomZeta("Te", -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Fitting.FixedPressureDrop("Filtre", -5)).isInstanceOf(IllegalArgumentException.class);
    }
}
