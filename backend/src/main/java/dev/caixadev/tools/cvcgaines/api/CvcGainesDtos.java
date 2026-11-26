package dev.caixadev.tools.cvcgaines.api;

import dev.caixadev.tools.cvcgaines.engine.Friction;
import dev.caixadev.tools.cvcgaines.engine.Material;
import dev.caixadev.tools.cvcgaines.engine.Regulatory;
import dev.caixadev.tools.cvcgaines.engine.Sizing;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Contrat JSON de l'API {@code cvc-gaines} v1.
 *
 * <p>Chaque champ porte son unite dans son nom ({@code flowM3h},
 * {@code lengthM}, {@code diameterMm}, {@code pressureDropPa}) : un client
 * (humain ou IA) n'a jamais a deviner l'unite. Les bornes de taille limitent
 * le cout d'un appel.
 */
public final class CvcGainesDtos {

    private CvcGainesDtos() {
    }

    // ─── Entrees communes ────────────────────────────────────────────────────

    @Schema(description = "Air transporte. Par defaut : 20 degC au niveau de la mer.")
    public record AirDto(
            @Schema(description = "Temperature, degC", example = "20") Double temperatureC,
            @Schema(description = "Altitude du site, m", example = "0") Double altitudeM) {
    }

    public enum ShapeKind { CIRCULAR, RECTANGULAR }

    @Schema(description = "Section interieure de la gaine. Circulaire : diameterMm ; rectangulaire : widthMm et heightMm.")
    public record ShapeDto(
            @NotNull ShapeKind kind,
            @Positive @Schema(example = "315") Double diameterMm,
            @Positive Double widthMm,
            @Positive Double heightMm) {
    }

    public enum FittingType {
        ROUND_ELBOW, MITERED_ROUND_ELBOW_90, SUDDEN_EXPANSION, SUDDEN_CONTRACTION,
        SHARP_ENTRY, FREE_EXIT, CUSTOM_ZETA, FIXED_PRESSURE_DROP
    }

    @Schema(description = """
            Accessoire porte par le troncon. Parametres selon le type :
            ROUND_ELBOW (radiusRatio, angleDeg), SUDDEN_EXPANSION et SUDDEN_CONTRACTION (areaRatio,
            petite section / grande section), CUSTOM_ZETA (label, zeta), FIXED_PRESSURE_DROP
            (label, pressureDropPa). Zeta rapporte a la pression dynamique du troncon.""")
    public record FittingDto(
            @NotNull FittingType type,
            @Positive Double radiusRatio,
            @Positive Double angleDeg,
            @Positive Double areaRatio,
            @Size(max = 100) String label,
            @PositiveOrZero Double zeta,
            @PositiveOrZero Double pressureDropPa) {
    }

    // ─── Troncon ─────────────────────────────────────────────────────────────

    @Schema(description = "Troncon a debit constant. Rugosite : roughnessMm si fourni, sinon material "
            + "(defaut GALVANIZED_LONGITUDINAL, 0,15 mm).")
    public record SectionRequest(
            @NotNull @Positive @Schema(example = "2000") Double flowM3h,
            @NotNull @PositiveOrZero @Schema(example = "10") Double lengthM,
            @NotNull @Valid ShapeDto shape,
            Material material,
            @PositiveOrZero Double roughnessMm,
            @Size(max = 50) List<@Valid @NotNull FittingDto> fittings,
            @Valid AirDto air) {
    }

    public record FittingResultDto(String label, double pressureDropPa) {
    }

    public record SectionResponse(
            double velocityMs,
            double dynamicPressurePa,
            double hydraulicDiameterMm,
            @Schema(description = "Diametre equivalent de Huebscher (rectangulaire uniquement)") Double equivalentDiameterMm,
            long reynolds,
            Friction.Regime regime,
            double frictionFactor,
            double linearPressureDropPaPerM,
            double linearPressureDropPa,
            double singularPressureDropPa,
            double totalPressureDropPa,
            List<FittingResultDto> fittings) {
    }

    // ─── Dimensionnement ─────────────────────────────────────────────────────

    @Schema(description = "Criteres : plus petite dimension normalisee respectant les deux limites. "
            + "Rectangulaire : heightMm impose, largeur choisie dans la serie NF EN 1505.")
    public record SizingCriteriaDto(
            @NotNull Sizing.ShapeKind kind,
            @NotNull @Positive @Schema(example = "7") Double maxVelocityMs,
            @NotNull @Positive @Schema(example = "1.5") Double maxPressureDropPaPerM,
            @Positive Double heightMm) {
    }

    public record SizingRequest(
            @NotNull @Positive @Schema(example = "2000") Double flowM3h,
            @NotNull @Valid SizingCriteriaDto criteria,
            Material material,
            @PositiveOrZero Double roughnessMm,
            @Valid AirDto air) {
    }

    public record ShapeResultDto(ShapeKind kind, Double diameterMm, Double widthMm, Double heightMm) {
    }

    public record SizingResponse(ShapeResultDto shape, double velocityMs, double pressureDropPaPerM,
                                 @Schema(description = "Critere qui a ecarte la dimension inferieure") String limitedBy) {
    }

    // ─── Reseau ──────────────────────────────────────────────────────────────

    @Schema(description = "Troncon d'un reseau arborescent. Un seul troncon sans parentId (raccord ventilateur). "
            + "terminalFlowM3h obligatoire sur les terminaux, interdit ailleurs. shape absent : dimensionne selon sizing.")
    public record NetworkSectionDto(
            @NotBlank @Size(max = 50) String id,
            @Size(max = 50) String parentId,
            @NotNull @PositiveOrZero Double lengthM,
            @Valid ShapeDto shape,
            Material material,
            @PositiveOrZero Double roughnessMm,
            @Size(max = 50) List<@Valid @NotNull FittingDto> fittings,
            @Positive Double terminalFlowM3h) {
    }

    public record NetworkRequest(
            @NotEmpty @Size(max = 500) List<@Valid @NotNull NetworkSectionDto> sections,
            @Valid SizingCriteriaDto sizing,
            @Valid AirDto air) {
    }

    public record NetworkSectionResultDto(String id, String parentId, double flowM3h, ShapeResultDto shape,
                                          boolean sized, String limitedBy, SectionResponse result) {
    }

    public record PathResultDto(String terminalId, List<String> path, double pressureDropPa,
                                @Schema(description = "Perte de charge a ajouter pour equilibrer sur le chemin critique, Pa")
                                double imbalancePa) {
    }

    public record NetworkResponse(double totalFlowM3h, String criticalTerminalId, double criticalPressureDropPa,
                                  List<NetworkSectionResultDto> sections, List<PathResultDto> paths) {
    }

    // ─── Debits reglementaires ───────────────────────────────────────────────

    public record DwellingRequest(
            @NotNull @Min(1) @Max(20) @Schema(example = "4") Integer mainRooms,
            @NotNull @Min(0) @Max(20) @Schema(example = "1") Integer bathrooms,
            @Min(0) @Max(20) Integer otherWaterRooms,
            @NotNull @Min(0) @Max(20) @Schema(example = "1") Integer toilets) {
    }

    public record WorkplaceRequest(
            @NotNull Regulatory.WorkplaceActivity activity,
            @NotNull @Min(1) @Max(100_000) @Schema(example = "12") Integer occupants) {
    }

    public record WorkplaceResponse(Regulatory.WorkplaceActivity activity, String label, int occupants,
                                    int flowPerOccupantM3h, int freshAirM3h, String reference) {
    }

    // ─── Catalogue ───────────────────────────────────────────────────────────

    public record MaterialDto(Material code, String label, double roughnessMm) {
    }

    public record ActivityDto(Regulatory.WorkplaceActivity code, String label, int flowPerOccupantM3h) {
    }

    public record CatalogResponse(List<MaterialDto> materials, List<FittingType> fittingTypes,
                                  List<Integer> circularDiametersMm, List<Integer> rectangularSidesMm,
                                  List<ActivityDto> workplaceActivities, AirDto defaultAir) {
    }
}
