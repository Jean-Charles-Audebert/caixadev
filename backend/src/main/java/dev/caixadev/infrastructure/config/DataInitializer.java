package dev.caixadev.infrastructure.config;

import dev.caixadev.domain.port.out.PasswordHashPort;
import dev.caixadev.infrastructure.persistence.entity.UserEntity;
import dev.caixadev.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provisionnement du superadministrateur au démarrage.
 *
 * - Les valeurs viennent exclusivement de l'environnement
 *   ({@code SUPER_ADMIN_EMAIL}, {@code SUPER_ADMIN_PASSWORD}).
 * - Opération idempotente : un compte existant n'est jamais recréé ni modifié.
 * - Aucune valeur sensible n'est journalisée (ni email, ni mot de passe).
 * - En production, l'absence de ces variables fait échouer le démarrage
 *   (contrôlé par {@code StartupSecurityValidator}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class DataInitializer implements ApplicationRunner {

    @Value("${app.super-admin.email:}")
    private String superAdminEmail;

    @Value("${app.super-admin.password:}")
    private String superAdminPassword;

    private final UserJpaRepository userJpaRepository;
    private final PasswordHashPort passwordHashPort;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createSuperAdminIfAbsent();
    }

    private void createSuperAdminIfAbsent() {
        if (isBlank(superAdminEmail) || isBlank(superAdminPassword)) {
            log.warn("SUPER_ADMIN_EMAIL ou SUPER_ADMIN_PASSWORD non défini : "
                    + "aucun superadministrateur ne sera créé.");
            return;
        }
        var existing = userJpaRepository.findByEmail(superAdminEmail);
        if (existing.isPresent()) {
            if (existing.get().isSuperAdmin()) {
                log.debug("Superadministrateur déjà présent : aucun changement.");
            } else {
                // Aucune élévation automatique de privilège : un compte existant n'est
                // jamais promu par la simple configuration de SUPER_ADMIN_EMAIL.
                log.warn("SUPER_ADMIN_EMAIL correspond à un compte existant non superadministrateur : "
                        + "aucune élévation de privilège effectuée. Vérifiez la configuration.");
            }
            return;
        }
        UserEntity admin = UserEntity.builder()
                .email(superAdminEmail)
                .password(passwordHashPort.encode(superAdminPassword))
                .displayName("SuperAdmin")
                .isSuperAdmin(true)
                .build();
        userJpaRepository.save(admin);
        log.info("Superadministrateur provisionné.");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
