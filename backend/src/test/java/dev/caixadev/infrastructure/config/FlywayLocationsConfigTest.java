package dev.caixadev.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Régression P3-06 : les données de démonstration (db/seed) ne doivent être
 * chargées que par le profil dev. En production et en test, seules les
 * migrations de db/migration sont appliquées.
 *
 * Le comportement réel de Flyway est couvert par
 * {@link FlywayMigrationScenariosTest} ; ce test vérifie le câblage des
 * fichiers de configuration.
 */
class FlywayLocationsConfigTest {

    @Test
    void productionDoesNotLoadDemoSeed() throws IOException {
        assertThat(flywayLocations("application-prod.yml")).doesNotContain("db/seed");
    }

    @Test
    void testProfileDoesNotLoadDemoSeed() throws IOException {
        assertThat(flywayLocations("application-test.yml")).doesNotContain("db/seed");
    }

    @Test
    void developmentLoadsDemoSeed() throws IOException {
        assertThat(flywayLocations("application-dev.yml")).contains("db/seed");
    }

    /**
     * Flyway n'accepte que des types dans `ignore-migration-patterns`
     * (`versioned`, `repeatable`, `*`) : une valeur comme `2:missing` fait
     * échouer le démarrage ("Invalid migration type '2'"). Comme il n'existe
     * pas de tolérance ciblée par version, la seule option restante serait
     * `*:missing`, qui masquerait toute migration réellement absente. On
     * interdit donc les deux configurations, et la fragilité résiduelle de V2
     * est traitée par une procédure documentée (docs/infrastructure.md).
     */
    @Test
    void noProfileUsesIgnoreMigrationPatterns() throws IOException {
        assertThat(propertyOrNull("application-prod.yml", "spring.flyway.ignore-migration-patterns"))
                .as("aucun motif d'ignorance en production")
                .isNull();
        assertThat(propertyOrNull("application-test.yml", "spring.flyway.ignore-migration-patterns"))
                .as("aucun motif d'ignorance en test")
                .isNull();
        assertThat(propertyOrNull("application-dev.yml", "spring.flyway.ignore-migration-patterns"))
                .as("aucun motif d'ignorance en dev")
                .isNull();
    }

    private static String flywayLocations(String resourceName) throws IOException {
        return propertyOrNull(resourceName, "spring.flyway.locations");
    }

    private static String propertyOrNull(String resourceName, String key) throws IOException {
        Resource resource = new ClassPathResource(resourceName);
        assertThat(resource.exists()).as("ressource %s", resourceName).isTrue();

        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(resourceName, resource);
        assertThat(sources).isNotEmpty();
        PropertySource<?> source = sources.get(0);

        Object direct = source.getProperty(key);
        if (direct != null) {
            return direct.toString();
        }

        List<String> indexed = new ArrayList<>();
        for (int index = 0; ; index++) {
            Object value = source.getProperty(key + "[" + index + "]");
            if (value == null) {
                break;
            }
            indexed.add(value.toString());
        }
        return indexed.isEmpty() ? null : String.join(",", indexed);
    }
}
