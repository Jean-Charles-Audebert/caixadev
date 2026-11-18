package dev.caixadev.infrastructure.config;

import dev.caixadev.home.HomeApplication;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Vérifie les quatre scénarios Flyway sur des schémas PostgreSQL réellement
 * isolés (un schéma par test, supprimé ensuite). Le schéma public de la base
 * de test n'est jamais touché.
 *
 * Le déplacement de {@code V2__Seed_initial.sql} de {@code db/migration} vers
 * {@code db/seed} est valide parce que Flyway identifie une migration par son
 * nom de fichier et son checksum, pas par son chemin. La seule fragilité est
 * l'absence de résolution de V2 dans les profils qui ne chargent pas db/seed :
 * elle est prouvée par A-bis et documentée dans docs/infrastructure.md.
 */
@SpringBootTest(classes = HomeApplication.class)
@ActiveProfiles("test")
class FlywayMigrationScenariosTest {

    private static final String MIGRATIONS = "classpath:db/migration";
    private static final String MIGRATIONS_AND_SEED = "classpath:db/migration,classpath:db/seed";

    @Autowired
    private DataSource dataSource;

    private final List<String> createdSchemas = new ArrayList<>();

    @AfterEach
    void dropCreatedSchemas() {
        for (String schema : createdSchemas) {
            execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
        createdSchemas.clear();
    }

    /**
     * Scénario A — une base de développement qui a déjà exécuté V1 et V2 doit
     * redémarrer sans réexécution, même si V2 est désormais résolue depuis db/seed.
     */
    @Test
    void scenarioA_existingDevelopmentDatabaseValidatesAndDoesNotReplayV2() throws IOException {
        String schema = newSchema("scenario_a");
        Path legacyLocation = legacyMigrationsDirectory("V1__Schema_auth.sql", "V2__Seed_initial.sql");

        Flyway legacy = flyway(schema, filesystemLocation(legacyLocation));
        assertThat(legacy.migrate().migrationsExecuted)
                .as("la base historique contient V1 et V2")
                .isEqualTo(2);

        Flyway current = flyway(schema, MIGRATIONS_AND_SEED);
        assertThat(appliedVersions(current))
                .as("V1 et V2 sont déjà appliquées")
                .containsExactly("1", "2");
        assertThat(pendingVersions(current))
                .as("V2 résolue depuis db/seed n'est pas en attente ; seule V3 l'est")
                .containsExactly("3");

        MigrateResult result = current.migrate();
        assertThat(result.migrationsExecuted)
                .as("seule V3 doit être appliquée, V2 ne doit pas être rejouée")
                .isEqualTo(1);
        current.validate();
        assertThat(appliedVersions(current)).containsExactly("1", "2", "3");
        assertThat(count(schema, "SELECT count(*) FROM " + schema + ".projects WHERE key = 'test'"))
                .as("le seed de démonstration ne doit exister qu'une fois")
                .isEqualTo(1);
        assertThat(count(schema, "SELECT count(*) FROM " + schema + ".roles WHERE project_id IS NULL"))
                .isEqualTo(7);
        assertThat(current.info().current().getVersion().getVersion()).isEqualTo("3");
    }

    /**
     * Scénario A-bis — fragilité résiduelle, sans garde-fou magique : une base
     * qui garde V2 dans son historique et démarre avec un profil qui ne charge
     * pas db/seed échoue à la validation, et donc au démarrage.
     *
     * C'est le comportement attendu : Flyway n'accepte que des types dans
     * `ignore-migration-patterns` (`2:missing` est refusé au démarrage), et
     * `*:missing` masquerait toute migration réellement absente. La remédiation
     * est explicite : démarrer une fois avec le profil dev (qui résout V2), ou
     * recréer la base, ou `flyway repair` en connaissance de cause.
     */
    @Test
    void scenarioABis_productionLikeProfileFailsOnMissingDemoSeedAndRecoversWithDevLocations() throws IOException {
        String schema = newSchema("scenario_a_bis");
        Path legacyLocation = legacyMigrationsDirectory("V1__Schema_auth.sql", "V2__Seed_initial.sql");

        flyway(schema, filesystemLocation(legacyLocation)).migrate();

        Flyway productionLike = flyway(schema, MIGRATIONS);
        assertThatThrownBy(productionLike::validate)
                .as("une migration appliquée non résolue doit bloquer la validation")
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("not resolved locally");
        assertThatThrownBy(productionLike::migrate)
                .as("le démarrage réel (migrate) doit échouer, pas seulement validate()")
                .isInstanceOf(FlywayException.class);

        // Remédiation 1 : démarrer une fois avec les locations du profil dev.
        Flyway recovery = flyway(schema, MIGRATIONS_AND_SEED);
        assertThat(pendingVersions(recovery))
                .as("V2 est de nouveau résolue (donc plus « missing ») ; seule V3 est en attente")
                .containsExactly("3");
        assertThat(recovery.migrate().migrationsExecuted)
                .as("V3 doit pouvoir être appliquée après résolution de V2")
                .isEqualTo(1);
        recovery.validate();
        assertThat(appliedVersions(recovery)).containsExactly("1", "2", "3");
        assertThat(count(schema, "SELECT count(*) FROM " + schema + ".projects WHERE key = 'test'"))
                .as("aucune donnée existante ne doit être supprimée")
                .isEqualTo(1);
    }

    /**
     * Scénario B — base de production vierge : V1 puis V3, aucun seed de démo,
     * rôles globaux créés, validation OK.
     */
    @Test
    void scenarioB_blankProductionDatabaseAppliesV1AndV3WithoutDemoData() {
        String schema = newSchema("scenario_b");

        Flyway production = flyway(schema, MIGRATIONS);
        MigrateResult result = production.migrate();

        assertThat(result.migrationsExecuted).isEqualTo(2);
        assertThat(appliedVersions(production)).containsExactly("1", "3");
        production.validate();
        assertThat(count(schema, "SELECT count(*) FROM " + schema + ".roles WHERE project_id IS NULL"))
                .as("les 7 rôles globaux doivent être créés par V3")
                .isEqualTo(7);
        assertThat(count(schema, "SELECT count(*) FROM " + schema + ".projects"))
                .as("aucun projet de démonstration en production")
                .isZero();
    }

    /**
     * Scénario C — nouvelle base de développement : V1, V2 (démo), V3, sans doublon.
     */
    @Test
    void scenarioC_blankDevelopmentDatabaseAppliesSeedThenGlobalRoles() {
        String schema = newSchema("scenario_c");

        Flyway development = flyway(schema, MIGRATIONS_AND_SEED);
        MigrateResult result = development.migrate();

        assertThat(result.migrationsExecuted).isEqualTo(3);
        assertThat(appliedVersions(development)).containsExactly("1", "2", "3");
        development.validate();
        assertThat(count(schema, "SELECT count(*) FROM " + schema + ".roles WHERE project_id IS NULL"))
                .as("V3 est idempotente : pas de doublon des rôles déjà insérés par V2")
                .isEqualTo(7);
        assertThat(count(schema, "SELECT count(*) FROM " + schema + ".projects WHERE key = 'test'"))
                .isEqualTo(1);
        assertThat(count(schema, "SELECT count(*) FROM " + schema + ".roles WHERE project_id IS NOT NULL"))
                .isEqualTo(2);
    }

    /**
     * Scénario D — une future migration V4 dans db/migration s'applique après V3,
     * y compris sur une base de développement dont l'historique contient le seed V2.
     */
    @Test
    void scenarioD_futureMigrationAppliesAfterSeedAndGlobalRoles() throws IOException {
        String schema = newSchema("scenario_d");

        flyway(schema, MIGRATIONS_AND_SEED).migrate();

        Path futureLocation = Files.createTempDirectory("caixadev-future-v4");
        Files.writeString(futureLocation.resolve("V4__Probe_marker.sql"),
                "CREATE TABLE probe_marker (id BIGINT PRIMARY KEY);\n");

        Flyway withFuture = flyway(schema,
                MIGRATIONS_AND_SEED + "," + filesystemLocation(futureLocation));
        MigrateResult result = withFuture.migrate();

        assertThat(result.migrationsExecuted).isEqualTo(1);
        assertThat(appliedVersions(withFuture)).containsExactly("1", "2", "3", "4");
        withFuture.validate();
        assertThat(count(schema, "SELECT count(*) FROM " + schema + ".probe_marker")).isZero();
    }

    private String newSchema(String prefix) {
        String schema = "fw_" + prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        execute("CREATE SCHEMA " + schema);
        createdSchemas.add(schema);
        return schema;
    }

    private Flyway flyway(String schema, String locations) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .locations(locations.split(","))
                .load();
    }

    /**
     * Flyway exige un préfixe explicite pour un répertoire hors classpath, et
     * les barres obliques évitent les surprises d'échappement sous Windows.
     */
    private static String filesystemLocation(Path directory) {
        return "filesystem:" + directory.toAbsolutePath().toString().replace('\\', '/');
    }

    /**
     * Rejoue l'ancienne disposition du dépôt : un répertoire qui contient les
     * migrations indiquées, avec exactement les mêmes octets que le classpath.
     */
    private Path legacyMigrationsDirectory(String... fileNames) throws IOException {
        Path directory = Files.createTempDirectory("caixadev-legacy-v2");
        for (String fileName : fileNames) {
            Resource resource = new ClassPathResource(resolveSeedOrMigrationPath(fileName));
            assertThat(resource.exists()).as("%s doit être présente", fileName).isTrue();
            try (InputStream input = resource.getInputStream()) {
                Files.copy(input, directory.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return directory;
    }

    private static String resolveSeedOrMigrationPath(String fileName) {
        return fileName.startsWith("V2") ? "db/seed/" + fileName : "db/migration/" + fileName;
    }

    private static List<String> appliedVersions(Flyway flyway) {
        return versions(flyway.info().applied());
    }

    private static List<String> pendingVersions(Flyway flyway) {
        return versions(flyway.info().pending());
    }

    private static List<String> versions(MigrationInfo[] infos) {
        return Arrays.stream(infos)
                .map(info -> info.getVersion().getVersion())
                .toList();
    }

    private int count(String schema, String sql) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Requête de vérification échouée : " + sql, e);
        }
    }

    private void execute(String sql) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("Commande SQL échouée : " + sql, e);
        }
    }
}
