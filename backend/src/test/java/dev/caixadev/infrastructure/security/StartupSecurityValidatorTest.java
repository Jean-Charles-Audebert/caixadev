package dev.caixadev.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Vérifie les garde-fous de configuration exécutés au démarrage.
 * Aucune valeur sensible réelle n'est utilisée.
 *
 * Deux niveaux : appels directs à {@code validate()} pour les règles, et
 * démarrage réel d'un contexte Spring pour prouver que l'application refuse
 * bien de démarrer (et pas seulement qu'une méthode lève une exception).
 */
class StartupSecurityValidatorTest {

    private static final String STRONG_SECRET = "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6";
    private static final String DEV_SECRET = "dev-only-insecure-jwt-secret-change-me-32chars";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(StartupSecurityValidator.class);

    @Test
    void rejectsStartupWhenNoProfileIsActive() {
        StartupSecurityValidator validator = validator(new MockEnvironment(), STRONG_SECRET, "admin@example.test", "password");

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SPRING_PROFILES_ACTIVE");
    }

    @Test
    void rejectsMissingSecret() {
        assertThatThrownBy(() -> validator(env("dev"), "", "admin@example.test", "password").validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void rejectsTooShortSecret() {
        assertThatThrownBy(() -> validator(env("dev"), "trop-court", "admin@example.test", "password").validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    void rejectsKnownDevelopmentSecretInProduction() {
        assertThatThrownBy(() -> validator(env("prod"), DEV_SECRET, "admin@example.test", "password").validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("développement");
    }

    @Test
    void rejectsProductionWhenSuperAdminIsNotProvided() {
        assertThatThrownBy(() -> validator(env("prod"), STRONG_SECRET, "", "").validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SUPER_ADMIN");
    }

    @Test
    void rejectsMixedDevAndProdProfiles() {
        assertThatThrownBy(() -> validator(env("dev", "prod"), STRONG_SECRET, "admin@example.test", "password").validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dev");
    }

    @Test
    void acceptsProductionWithStrongSecretAndSuperAdmin() {
        assertThatCode(() -> validator(env("prod"), STRONG_SECRET, "admin@example.test", "password").validate())
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsDevelopmentWithLocalSecret() {
        assertThatCode(() -> validator(env("dev"), DEV_SECRET, "", "").validate())
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsPlaceholderSecretInProduction() {
        String placeholder = "change-me-please-with-a-long-random-value";

        assertThatThrownBy(() -> validator(env("prod"), placeholder, "admin@example.test", "password").validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("développement");
    }

    // ─── Démarrage réel du contexte (pas seulement l'appel de validate) ─────────

    @Test
    void springContextFailsToStartInProductionWithoutSecret() {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasStackTraceContaining("JWT_SECRET");
                });
    }

    @Test
    void springContextFailsToStartWithDevelopmentSecretInProduction() {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
                .withPropertyValues(
                        "jwt.secret=" + DEV_SECRET,
                        "app.super-admin.email=admin@example.test",
                        "app.super-admin.password=password"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("développement");
                });
    }

    @Test
    void springContextStartsInProductionWithStrongSecretAndSuperAdmin() {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
                .withPropertyValues(
                        "jwt.secret=" + STRONG_SECRET,
                        "app.super-admin.email=admin@example.test",
                        "app.super-admin.password=password"
                )
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void springContextStartsInDevelopmentWithLocalSecret() {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("dev"))
                .withPropertyValues("jwt.secret=" + DEV_SECRET)
                .run(context -> assertThat(context).hasNotFailed());
    }

    private static MockEnvironment env(String... profiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles);
        return environment;
    }

    private static StartupSecurityValidator validator(
            MockEnvironment environment,
            String secret,
            String email,
            String password
    ) {
        return new StartupSecurityValidator(environment, secret, email, password);
    }
}
