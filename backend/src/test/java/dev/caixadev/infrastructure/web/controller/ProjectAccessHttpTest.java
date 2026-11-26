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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Règle d'accès aux projets (A02 / T01).
 *
 * Le SUPER_ADMIN est un rôle global de plateforme : il voit, consulte et
 * administre tous les projets clients, qu'une affectation existe ou non dans
 * {@code user_project_roles}. Un utilisateur ordinaire reste limité aux projets
 * auxquels il est explicitement affecté.
 *
 * Volontairement sans {@code @Transactional} : la transaction de test masquerait
 * les erreurs de chargement paresseux et les appels HTTP réels.
 */
@SpringBootTest(
        classes = HomeApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class ProjectAccessHttpTest {

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

    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdProjectIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        transactionTemplate.executeWithoutResult(status -> {
            // Le projet supprime en cascade ses rôles et ses affectations.
            createdProjectIds.forEach(projectJpaRepository::deleteById);
            createdUserIds.forEach(userJpaRepository::deleteById);
        });
        createdProjectIds.clear();
        createdUserIds.clear();
    }

    // ---------------------------------------------------------------------
    // SUPER_ADMIN : accès global, sans affectation
    // ---------------------------------------------------------------------

    @Test
    void superAdminWithoutAssignmentSeesAllActiveProjects() {
        UserEntity superAdmin = createUser(true);
        ProjectEntity active = createProject(true);
        ProjectEntity inactive = createProject(false);

        ResponseEntity<List<Map<String, Object>>> response = getMyProjects(superAdmin);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> keys = keysOf(response.getBody());
        assertThat(keys).contains(active.getKey());
        assertThat(keys).doesNotContain(inactive.getKey());
        // Tous les projets actifs de la base, ni plus ni moins.
        assertThat(keys).hasSameSizeAs(projectJpaRepository.findAllByIsActiveTrue());
        // Aucun rôle de projet n'est inventé pour le rôle global.
        assertThat(rolesOf(projectByKey(response.getBody(), active.getKey()))).isEmpty();
    }

    @Test
    void superAdminConsultsProjectWithoutAssignment() {
        UserEntity superAdmin = createUser(true);
        ProjectEntity project = createProject(true);

        ResponseEntity<List<Map<String, Object>>> response = getMyProjects(superAdmin);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> consulted = projectByKey(response.getBody(), project.getKey());
        assertThat(consulted).isNotNull();
        assertThat(consulted.get("name")).isEqualTo(project.getName());
        assertThat(consulted.get("description")).isEqualTo(project.getDescription());
        assertThat(consulted.get("isActive")).isEqualTo(true);
        assertThat(consulted.get("isPublic")).isEqualTo(false);
        assertThat(rolesOf(consulted)).isEmpty();

        // Consultation côté administration : la liste globale inclut le projet.
        ResponseEntity<List<Map<String, Object>>> adminResponse = restTemplate.exchange(
                "/api/admin/projects",
                HttpMethod.GET,
                new HttpEntity<>(accessTokenHeaders(superAdmin)),
                new ParameterizedTypeReference<>() {
                }
        );
        assertThat(adminResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(keysOf(adminResponse.getBody())).contains(project.getKey());
    }

    @Test
    void superAdminAdministersProjectWithoutAssignment() {
        UserEntity superAdmin = createUser(true);
        UserEntity targetUser = createUser(false);
        ProjectEntity project = createProject(true);
        RoleEntity role = createRole(project, "ADMIN");

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/admin/users/" + targetUser.getId() + "/roles",
                HttpMethod.POST,
                new HttpEntity<>(
                        Map.of("projectId", project.getId(), "roleId", role.getId()),
                        accessTokenHeaders(superAdmin)),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        transactionTemplate.executeWithoutResult(status -> {
            assertThat(userProjectRoleJpaRepository.findByUserId(targetUser.getId()))
                    .singleElement()
                    .satisfies(assignment -> {
                        assertThat(assignment.getProject().getId()).isEqualTo(project.getId());
                        assertThat(assignment.getRole().getName()).isEqualTo("ADMIN");
                    });
            // Le superadministrateur n'a lui-même aucune affectation : l'accès
            // ne provient pas d'une ligne dans user_project_roles.
            assertThat(userProjectRoleJpaRepository.findByUserId(superAdmin.getId())).isEmpty();
        });
    }

    // ---------------------------------------------------------------------
    // Utilisateurs ordinaires : restrictions inchangées
    // ---------------------------------------------------------------------

    @Test
    void ordinaryUserSeesOnlyAssignedProjects() {
        UserEntity user = createUser(false);
        ProjectEntity assigned = createProject(true);
        ProjectEntity notAssigned = createProject(true);
        RoleEntity role = createRole(assigned, "MEMBER");
        assignRole(user, assigned, role);

        ResponseEntity<List<Map<String, Object>>> response = getMyProjects(user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(keysOf(response.getBody())).containsExactly(assigned.getKey());
        assertThat(keysOf(response.getBody())).doesNotContain(notAssigned.getKey());
        assertThat(rolesOf(response.getBody().get(0))).containsExactly("MEMBER");
    }

    @Test
    void ordinaryUserCannotReachProjectEndpointsOfAnotherProject() {
        UserEntity user = createUser(false);
        ProjectEntity unauthorized = createProject(true);
        RoleEntity role = createRole(unauthorized, "ADMIN");

        // La liste globale est réservée au SUPER_ADMIN.
        ResponseEntity<String> listAll = restTemplate.exchange(
                "/api/admin/projects",
                HttpMethod.GET,
                new HttpEntity<>(accessTokenHeaders(user)),
                String.class
        );
        assertThat(listAll.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // L'administration d'un projet (affectation de rôle) aussi.
        ResponseEntity<String> adminAssign = restTemplate.exchange(
                "/api/admin/users/" + user.getId() + "/roles",
                HttpMethod.POST,
                new HttpEntity<>(
                        Map.of("projectId", unauthorized.getId(), "roleId", role.getId()),
                        accessTokenHeaders(user)),
                String.class
        );
        assertThat(adminAssign.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        transactionTemplate.executeWithoutResult(status ->
                assertThat(userProjectRoleJpaRepository.findByUserId(user.getId())).isEmpty());
    }

    @Test
    void ordinaryUsersStayIsolatedFromEachOther() {
        UserEntity alice = createUser(false);
        UserEntity bob = createUser(false);
        ProjectEntity aliceProject = createProject(true);
        ProjectEntity bobProject = createProject(true);

        assignRole(alice, aliceProject, createRole(aliceProject, "ADMIN"));
        assignRole(bob, bobProject, createRole(bobProject, "MEMBER"));

        assertThat(keysOf(getMyProjects(alice).getBody())).containsExactly(aliceProject.getKey());
        assertThat(keysOf(getMyProjects(bob).getBody())).containsExactly(bobProject.getKey());
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private ResponseEntity<List<Map<String, Object>>> getMyProjects(UserEntity user) {
        return restTemplate.exchange(
                "/api/users/me/projects",
                HttpMethod.GET,
                new HttpEntity<>(accessTokenHeaders(user)),
                new ParameterizedTypeReference<>() {
                }
        );
    }

    private UserEntity createUser(boolean superAdmin) {
        UserEntity user = transactionTemplate.execute(status -> userJpaRepository.save(
                UserEntity.builder()
                        .email("access-" + UUID.randomUUID() + "@example.test")
                        .password("not-used")
                        .displayName(superAdmin ? "Super administrateur" : "Utilisateur ordinaire")
                        .isSuperAdmin(superAdmin)
                        .build()
        ));
        assertThat(user).isNotNull();
        createdUserIds.add(user.getId());
        return user;
    }

    private ProjectEntity createProject(boolean active) {
        ProjectEntity project = transactionTemplate.execute(status -> projectJpaRepository.save(
                ProjectEntity.builder()
                        .key("access-" + UUID.randomUUID())
                        .name(active ? "Projet actif" : "Projet inactif")
                        .description("Projet créé par un test d'accès")
                        .isActive(active)
                        .isPublic(false)
                        .build()
        ));
        assertThat(project).isNotNull();
        createdProjectIds.add(project.getId());
        return project;
    }

    private RoleEntity createRole(ProjectEntity project, String name) {
        RoleEntity role = transactionTemplate.execute(status -> {
            ProjectEntity managed = projectJpaRepository.findById(project.getId()).orElseThrow();
            return roleJpaRepository.save(RoleEntity.builder().project(managed).name(name).build());
        });
        assertThat(role).isNotNull();
        return role;
    }

    private void assignRole(UserEntity user, ProjectEntity project, RoleEntity role) {
        transactionTemplate.executeWithoutResult(status -> {
            UserEntity managedUser = userJpaRepository.findById(user.getId()).orElseThrow();
            ProjectEntity managedProject = projectJpaRepository.findById(project.getId()).orElseThrow();
            RoleEntity managedRole = roleJpaRepository.findById(role.getId()).orElseThrow();
            userProjectRoleJpaRepository.save(UserProjectRoleEntity.builder()
                    .id(new UserProjectRoleId(managedUser.getId(), managedProject.getId(), managedRole.getId()))
                    .user(managedUser)
                    .project(managedProject)
                    .role(managedRole)
                    .build());
        });
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
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private static List<String> keysOf(List<Map<String, Object>> projects) {
        return projects.stream().map(project -> (String) project.get("key")).toList();
    }

    @SuppressWarnings("unchecked")
    private static List<String> rolesOf(Map<String, Object> project) {
        assertThat(project).isNotNull();
        return (List<String>) project.get("roles");
    }

    private static Map<String, Object> projectByKey(List<Map<String, Object>> projects, String key) {
        return projects.stream()
                .filter(project -> key.equals(project.get("key")))
                .findFirst()
                .orElse(null);
    }
}
