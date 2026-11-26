package dev.caixadev.infrastructure.config;

import dev.caixadev.domain.port.out.PasswordHashPort;
import dev.caixadev.home.HomeApplication;
import dev.caixadev.infrastructure.persistence.entity.UserEntity;
import dev.caixadev.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Provisionnement idempotent du superadministrateur (P3-03).
 * Le mot de passe est fictif et ne correspond à aucun environnement réel.
 */
@SpringBootTest(classes = HomeApplication.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.super-admin.email=superadmin-idempotence@example.test",
        "app.super-admin.password=IdempotenceTestPassword-123456"
})
class DataInitializerIdempotenceTest {

    private static final String EMAIL = "superadmin-idempotence@example.test";

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PasswordHashPort passwordHashPort;

    private Long createdUserId;

    @AfterEach
    void cleanUp() {
        if (createdUserId != null) {
            userJpaRepository.deleteById(createdUserId);
            createdUserId = null;
        }
    }

    @Test
    void createsSuperAdminExactlyOnce() {
        var arguments = new DefaultApplicationArguments(new String[0]);

        dataInitializer.run(arguments);
        dataInitializer.run(arguments);

        List<UserEntity> accounts = userJpaRepository.findAll().stream()
                .filter(user -> EMAIL.equals(user.getEmail()))
                .toList();

        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).isSuperAdmin()).isTrue();
    }

    @Test
    void doesNotElevateAnExistingRegularAccount() {
        String email = "regular-" + UUID.randomUUID() + "@example.test";
        UserEntity regular = userJpaRepository.save(UserEntity.builder()
                .email(email)
                .password(passwordHashPort.encode("IrrelevantPassword-123"))
                .displayName("Compte ordinaire")
                .isSuperAdmin(false)
                .build());
        createdUserId = regular.getId();

        DataInitializer initializer = new DataInitializer(userJpaRepository, passwordHashPort);
        ReflectionTestUtils.setField(initializer, "superAdminEmail", email);
        ReflectionTestUtils.setField(initializer, "superAdminPassword", "IdempotenceTestPassword-123456");

        initializer.run(new DefaultApplicationArguments(new String[0]));

        UserEntity reloaded = userJpaRepository.findById(regular.getId()).orElseThrow();
        assertThat(reloaded.isSuperAdmin())
                .as("un compte existant ne doit jamais être promu par simple configuration")
                .isFalse();
        assertThat(userJpaRepository.findAll().stream()
                .filter(user -> email.equals(user.getEmail()))
                .count())
                .isEqualTo(1);
    }
}
