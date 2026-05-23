package dev.caixadev.infrastructure.config;

import dev.caixadev.domain.port.out.PasswordHashPort;
import dev.caixadev.infrastructure.persistence.entity.ProjectEntity;
import dev.caixadev.infrastructure.persistence.entity.RoleEntity;
import dev.caixadev.infrastructure.persistence.entity.UserEntity;
import dev.caixadev.infrastructure.persistence.entity.UserProjectRoleEntity;
import dev.caixadev.infrastructure.persistence.entity.UserProjectRoleId;
import dev.caixadev.infrastructure.persistence.repository.ProjectJpaRepository;
import dev.caixadev.infrastructure.persistence.repository.RoleJpaRepository;
import dev.caixadev.infrastructure.persistence.repository.UserJpaRepository;
import dev.caixadev.infrastructure.persistence.repository.UserProjectRoleJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Initialisation des données au démarrage.
 * - Toujours : crée le SuperAdmin si absent (email + password depuis les variables d'env).
 * - Profil dev uniquement : crée des utilisateurs de test avec rôles.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class DataInitializer implements ApplicationRunner {

    @Value("${app.super-admin.email}")
    private String superAdminEmail;

    @Value("${app.super-admin.password:}")
    private String superAdminPassword;

    private final UserJpaRepository userJpaRepository;
    private final ProjectJpaRepository projectJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final UserProjectRoleJpaRepository userProjectRoleJpaRepository;
    private final PasswordHashPort passwordHashPort;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createSuperAdminIfAbsent();
    }

    private void createSuperAdminIfAbsent() {
        if (superAdminPassword == null || superAdminPassword.isBlank()) {
            log.warn("⚠️  SUPER_ADMIN_PASSWORD non défini — SuperAdmin non créé. Renseignez SUPER_ADMIN_PASSWORD dans le .env.");
            return;
        }
        if (userJpaRepository.existsByEmail(superAdminEmail)) {
            log.debug("SuperAdmin déjà présent : {}", superAdminEmail);
            return;
        }
        UserEntity admin = UserEntity.builder()
                .email(superAdminEmail)
                .password(passwordHashPort.encode(superAdminPassword))
                .displayName("SuperAdmin")
                .isSuperAdmin(true)
                .build();
        userJpaRepository.save(admin);
        log.info("✅ SuperAdmin créé : {}", superAdminEmail);
    }
}

