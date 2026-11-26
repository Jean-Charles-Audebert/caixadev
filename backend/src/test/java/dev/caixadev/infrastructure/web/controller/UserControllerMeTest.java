package dev.caixadev.infrastructure.web.controller;

import dev.caixadev.domain.model.User;
import dev.caixadev.home.HomeApplication;
import dev.caixadev.infrastructure.persistence.entity.UserEntity;
import dev.caixadev.infrastructure.persistence.repository.UserJpaRepository;
import dev.caixadev.infrastructure.security.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de /api/users/me — AUTH-003.
 * Le profil retourné est toujours celui de l'utilisateur authentifié (aucun identifiant en entrée).
 */
@SpringBootTest(classes = HomeApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerMeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void standardUserGetsOwnProfile() throws Exception {
        UserEntity user = createUser("standard", false);

        mockMvc.perform(get("/api/users/me").cookie(accessTokenCookie(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.displayName").value("standard"));
    }

    @Test
    void superAdminGetsOwnProfile() throws Exception {
        UserEntity admin = createUser("superadmin", true);

        mockMvc.perform(get("/api/users/me").cookie(accessTokenCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(admin.getId()))
                .andExpect(jsonPath("$.email").value(admin.getEmail()));
    }

    @Test
    void unknownUserReturnsNotFound() throws Exception {
        User ghost = new User(999_999_999L, "ghost@example.com", null, "Ghost", false, null);
        Cookie cookie = new Cookie("access_token", jwtService.generateAccessToken(ghost));

        mockMvc.perform(get("/api/users/me").cookie(cookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    /**
     * Non-régression IDOR : aucun paramètre ne permet de cibler un autre compte.
     * Un identifiant fourni en paramètre est ignoré, le profil renvoyé reste celui du porteur du token.
     */
    @Test
    void meNeverReturnsAnotherUserProfile() throws Exception {
        UserEntity user = createUser("owner", false);
        UserEntity other = createUser("other", false);

        mockMvc.perform(get("/api/users/me")
                        .param("id", other.getId().toString())
                        .cookie(accessTokenCookie(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value(user.getEmail()));
    }

    private UserEntity createUser(String displayName, boolean superAdmin) {
        return userJpaRepository.save(UserEntity.builder()
                .email(displayName + "-" + UUID.randomUUID() + "@example.com")
                .password("not-used-in-this-test")
                .displayName(displayName)
                .isSuperAdmin(superAdmin)
                .build());
    }

    private Cookie accessTokenCookie(UserEntity entity) {
        User domain = new User(
                entity.getId(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getDisplayName(),
                entity.isSuperAdmin(),
                entity.getCreatedAt()
        );
        return new Cookie("access_token", jwtService.generateAccessToken(domain));
    }
}
