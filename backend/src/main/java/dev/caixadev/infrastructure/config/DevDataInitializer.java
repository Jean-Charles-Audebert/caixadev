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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Utilisateurs et données de test — profil dev uniquement.
 *
 * Utilisateurs créés :
 *   alice@test.com  / Test1234!  → ADMIN  sur projet 'test'
 *   bob@test.com    / Test1234!  → MEMBER sur projet 'test'
 *   carol@test.com  / Test1234!  → aucun projet (dashboard vide)
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
@Order(2)
public class DevDataInitializer implements ApplicationRunner {

    private static final String DEFAULT_PASSWORD = "Test1234!";

    private final UserJpaRepository userJpaRepository;
    private final ProjectJpaRepository projectJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final UserProjectRoleJpaRepository userProjectRoleJpaRepository;
    private final PasswordHashPort passwordHashPort;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Optional<ProjectEntity> testProject = projectJpaRepository.findByKey("test");
        if (testProject.isEmpty()) {
            log.warn("Projet 'test' introuvable — les utilisateurs de test ne seront pas assignés à un projet.");
        }

        UserEntity alice = createUserIfAbsent("alice@test.com", "Alice Dupont");
        UserEntity bob   = createUserIfAbsent("bob@test.com",   "Bob Martin");
                          createUserIfAbsent("carol@test.com",  "Carol Leroy");

        testProject.ifPresent(project -> {
            assignRole(alice, project, "ADMIN");
            assignRole(bob,   project, "MEMBER");
        });

        log.info("✅ Utilisateurs de test initialisés (profil dev)");
    }

    private UserEntity createUserIfAbsent(String email, String displayName) {
        return userJpaRepository.findByEmail(email).orElseGet(() -> {
            UserEntity user = UserEntity.builder()
                    .email(email)
                    .password(passwordHashPort.encode(DEFAULT_PASSWORD))
                    .displayName(displayName)
                    .isSuperAdmin(false)
                    .build();
            UserEntity saved = userJpaRepository.save(user);
            log.info("  → Utilisateur créé : {} ({})", email, displayName);
            return saved;
        });
    }

    private void assignRole(UserEntity user, ProjectEntity project, String roleName) {
        // Recherche le rôle spécifique au projet (inséré par V2__Seed_initial.sql)
        Optional<RoleEntity> roleOpt = roleJpaRepository
                .findAll()
                .stream()
                .filter(r -> r.getName().equals(roleName)
                        && r.getProject() != null
                        && r.getProject().getId().equals(project.getId()))
                .findFirst();

        if (roleOpt.isEmpty()) {
            log.warn("  ⚠️  Rôle '{}' introuvable sur le projet '{}' — assignation ignorée", roleName, project.getKey());
            return;
        }

        RoleEntity role = roleOpt.get();
        UserProjectRoleId id = new UserProjectRoleId(user.getId(), project.getId(), role.getId());

        if (!userProjectRoleJpaRepository.existsById(id)) {
            userProjectRoleJpaRepository.save(
                    UserProjectRoleEntity.builder()
                            .id(id)
                            .user(user)
                            .project(project)
                            .role(role)
                            .build()
            );
            log.info("  → Rôle '{}' assigné à {} sur projet '{}'", roleName, user.getEmail(), project.getKey());
        }
    }
}

