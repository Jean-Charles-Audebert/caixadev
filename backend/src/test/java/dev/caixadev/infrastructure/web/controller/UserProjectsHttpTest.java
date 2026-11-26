package dev.caixadev.infrastructure.web.controller;

import dev.caixadev.domain.model.User;
import dev.caixadev.home.HomeApplication;
import dev.caixadev.infrastructure.persistence.entity.ProjectEntity;
import dev.caixadev.infrastructure.persistence.entity.RoleEntity;
import dev.caixadev.infrastructure.persistence.entity.UserEntity;
import dev.caixadev.infrastructure.persistence.entity.UserProjectRoleEntity;
import dev.caixadev.infrastructure.persistence.entity.UserProjectRoleId;
import dev.caixadev.infrastructure.persistence.repository.ProjectJpaRepository;
import dev.caixadev.infrastructure.persistence.repository.RoleJpaRepository;
import dev.caixadev.infrastructure.persistence.repository.UserJpaRepository;
import dev.caixadev.infrastructure.persistence.repository.UserProjectRoleJpaRepository;
import dev.caixadev.infrastructure.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test d'intégration HTTP réel de {@code GET /api/users/me/projects}.
 *
 * Volontairement sans {@code @Transactional} : la transaction de test masquerait
 * les erreurs de chargement paresseux (LazyInitializationException), qui
 * n'apparaissent qu'en dehors d'une session Hibernate ouverte.
 */
@SpringBootTest(
        classes = HomeApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class UserProjectsHttpTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private ProjectJpaRepository projectJpaRepository;

    @Autowired
    private RoleJpaRepository roleJpaRepository;

    @Autowired
    private UserProjectRoleJpaRepository userProjectRoleJpaRepository;

    @Autowired
    private JwtService jwtService;

    private Long createdUserId;
    private Long createdProjectId;

    @AfterEach
    void cleanUp() {
        transactionTemplate.executeWithoutResult(status -> {
            if (createdProjectId != null) {
                // Le projet supprime en cascade le rôle de projet et l'affectation.
                projectJpaRepository.deleteById(createdProjectId);
            }
            if (createdUserId != null) {
                userJpaRepository.deleteById(createdUserId);
            }
        });
    }

    @Test
    void userWithAssignmentReceivesProjectsWithRoles() {
        String projectKey = "audit-" + UUID.randomUUID();
        UserEntity user = createUser();

        transactionTemplate.executeWithoutResult(status -> {
            ProjectEntity project = projectJpaRepository.save(ProjectEntity.builder()
                    .key(projectKey)
                    .name("Projet audité")
                    .description("Projet créé par un test d'intégration")
                    .isActive(true)
                    .isPublic(false)
                    .build());
            createdProjectId = project.getId();

            RoleEntity role = roleJpaRepository.save(RoleEntity.builder()
                    .project(project)
                    .name("ADMIN")
                    .build());

            userProjectRoleJpaRepository.save(UserProjectRoleEntity.builder()
                    .id(new UserProjectRoleId(user.getId(), project.getId(), role.getId()))
                    .user(user)
                    .project(project)
                    .role(role)
                    .build());
        });

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                "/api/users/me/projects",
                HttpMethod.GET,
                new HttpEntity<>(accessTokenHeaders(user)),
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);

        Map<String, Object> project = response.getBody().get(0);
        assertThat(project.get("key")).isEqualTo(projectKey);
        assertThat(project.get("name")).isEqualTo("Projet audité");
        assertThat(rolesOf(project)).containsExactly("ADMIN");
    }

    @Test
    void userWithoutAssignmentReceivesEmptyList() {
        UserEntity user = createUser();

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                "/api/users/me/projects",
                HttpMethod.GET,
                new HttpEntity<>(accessTokenHeaders(user)),
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static List<String> rolesOf(Map<String, Object> project) {
        return (List<String>) project.get("roles");
    }

    private UserEntity createUser() {
        UserEntity user = transactionTemplate.execute(status -> userJpaRepository.save(
                UserEntity.builder()
                        .email("projects-" + UUID.randomUUID() + "@example.test")
                        .password("not-used")
                        .displayName("Utilisateur projets")
                        .isSuperAdmin(false)
                        .build()
        ));
        assertThat(user).isNotNull();
        createdUserId = user.getId();
        return user;
    }

    private HttpHeaders accessTokenHeaders(UserEntity entity) {
        User domain = new User(
                entity.getId(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getDisplayName(),
                entity.isSuperAdmin(),
                entity.getCreatedAt()
        );
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "access_token=" + jwtService.generateAccessToken(domain));
        return headers;
    }
}
