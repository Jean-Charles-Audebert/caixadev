package dev.caixadev.tools.cvcgaines.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegulatoryTest {

    @ParameterizedTest(name = "T{0}")
    // pieces principales, cuisine, salle de bains, total art. 4, cuisine reduite art. 4
    @CsvSource({"1,75,15,35,20", "2,90,15,60,30", "3,105,30,75,45", "4,120,30,90,45",
                "5,135,30,105,45", "6,135,30,120,45", "7,135,30,135,45", "9,135,30,135,45"})
    void dwellingTablesByNumberOfMainRooms(int rooms, int kitchen, int bathroom, int minTotal, int minKitchen) {
        Regulatory.DwellingResult r = Regulatory.dwelling(new Regulatory.Dwelling(rooms, 1, 0, 1));

        assertThat(r.rooms().get(0)).isEqualTo(new Regulatory.RoomFlow("Cuisine", kitchen));
        assertThat(r.rooms().get(1)).isEqualTo(new Regulatory.RoomFlow("Salle de bains", bathroom));
        assertThat(r.minimumTotalM3h()).isEqualTo(minTotal);
        assertThat(r.minimumKitchenM3h()).isEqualTo(minKitchen);
        assertThat(r.totalExtractM3h()).isEqualTo(r.rooms().stream().mapToInt(Regulatory.RoomFlow::flowM3h).sum());
    }

    @Test
    void toiletsSwitchToMultipleFromTwo() {
        Regulatory.DwellingResult single = Regulatory.dwelling(new Regulatory.Dwelling(4, 1, 0, 1));
        Regulatory.DwellingResult multiple = Regulatory.dwelling(new Regulatory.Dwelling(4, 1, 0, 2));

        assertThat(single.rooms()).contains(new Regulatory.RoomFlow("WC (unique)", 30));
        assertThat(multiple.rooms()).filteredOn(f -> f.room().startsWith("WC"))
                .containsExactly(new Regulatory.RoomFlow("WC (multiples)", 15), new Regulatory.RoomFlow("WC (multiples)", 15));
        // T4, SdB + 2 WC : 120 + 30 + 15 + 15.
        assertThat(multiple.totalExtractM3h()).isEqualTo(180);
    }

    @Test
    void secondBathroomCountsAsOtherWaterRoom() {
        Regulatory.DwellingResult r = Regulatory.dwelling(new Regulatory.Dwelling(5, 2, 1, 2));
        assertThat(r.rooms()).filteredOn(f -> f.room().equals("Autre salle d'eau")).hasSize(2);
        // 135 + 30 + 15 + 15 + 15 + 15
        assertThat(r.totalExtractM3h()).isEqualTo(225);
    }

    @Test
    void workplaceFreshAirPerOccupant() {
        assertThat(Regulatory.workplace(Regulatory.WorkplaceActivity.OFFICE, 12).freshAirM3h()).isEqualTo(300);
        assertThat(Regulatory.workplace(Regulatory.WorkplaceActivity.CATERING_SALES_MEETING, 20).freshAirM3h()).isEqualTo(600);
        assertThat(Regulatory.workplace(Regulatory.WorkplaceActivity.LIGHT_PHYSICAL_WORK, 2).freshAirM3h()).isEqualTo(90);
        assertThat(Regulatory.workplace(Regulatory.WorkplaceActivity.OTHER_WORKSHOP, 1).freshAirM3h()).isEqualTo(60);
    }

    @Test
    void invalidInputsAreRejected() {
        assertThatThrownBy(() -> new Regulatory.Dwelling(0, 1, 0, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Regulatory.Dwelling(3, -1, 0, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Regulatory.workplace(Regulatory.WorkplaceActivity.OFFICE, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
