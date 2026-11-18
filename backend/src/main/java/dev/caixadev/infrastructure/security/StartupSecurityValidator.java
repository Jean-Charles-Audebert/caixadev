package dev.caixadev.infrastructure.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Contrôles de configuration exécutés au démarrage.
 *
 * Objectifs :
 * - rendre le profil Spring explicite (aucun profil implicite, donc jamais `dev`
 *   par accident dans un environnement d'exécution) ;
 * - refuser en production un secret JWT absent, trop court ou connu comme valeur
 *   de développement ;
 * - refuser en production un profil mixte `dev` + `prod` ;
 * - exiger en production les variables de provisionnement du superadministrateur.
 *
 * Les valeurs des secrets ne sont jamais journalisées.
 */
@Slf4j
@Component
public class StartupSecurityValidator {

    /** HS256 exige une clé d'au moins 256 bits, soit 32 octets. */
    static final int MIN_SECRET_BYTES = 32;

    /** Valeurs de développement connues, interdites en production. */
    static final Set<String> KNOWN_DEV_SECRETS = Set.of(
            "changeme-this-is-a-very-long-secret-key-for-dev-only-32chars",
            "dev-only-insecure-jwt-secret-change-me-32chars",
            "ci-test-secret-key-at-least-32-chars-long"
    );

    /** Fragments typiques d'une valeur non renseignée. */
    static final List<String> WEAK_SECRET_MARKERS = List.of(
            "changeme", "change-me", "dev-only", "for-dev", "insecure", "example", "placeholder"
    );

    private final Environment environment;
    private final String jwtSecret;
    private final String superAdminEmail;
    private final String superAdminPassword;

    public StartupSecurityValidator(
            Environment environment,
            @Value("${jwt.secret:}") String jwtSecret,
            @Value("${app.super-admin.email:}") String superAdminEmail,
            @Value("${app.super-admin.password:}") String superAdminPassword
    ) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
        this.superAdminEmail = superAdminEmail;
        this.superAdminPassword = superAdminPassword;
    }

    @PostConstruct
    void validate() {
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        if (profiles.isEmpty()) {
            throw new IllegalStateException(
                    "SPRING_PROFILES_ACTIVE doit être défini explicitement (dev, test ou prod). "
                            + "Aucun profil n'est activé par défaut.");
        }

        boolean production = profiles.contains("prod");

        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET est absent. Définissez la variable d'environnement JWT_SECRET.");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET est trop court : " + MIN_SECRET_BYTES + " octets minimum.");
        }

        if (production) {
            if (profiles.contains("dev")) {
                throw new IllegalStateException(
                        "Le profil 'dev' ne peut pas être actif en production.");
            }
            if (KNOWN_DEV_SECRETS.contains(jwtSecret) || looksLikePlaceholder(jwtSecret)) {
                throw new IllegalStateException(
                        "JWT_SECRET utilise une valeur de développement connue : "
                                + "générez un secret aléatoire dédié à la production.");
            }
            if (isBlank(superAdminEmail) || isBlank(superAdminPassword)) {
                throw new IllegalStateException(
                        "SUPER_ADMIN_EMAIL et SUPER_ADMIN_PASSWORD doivent être définis en production "
                                + "pour provisionner le superadministrateur.");
            }
        }

        log.info("Configuration de sécurité validée (profils actifs : {})", profiles);
    }

    private static boolean looksLikePlaceholder(String secret) {
        String normalized = secret.toLowerCase(Locale.ROOT);
        return WEAK_SECRET_MARKERS.stream().anyMatch(normalized::contains);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
