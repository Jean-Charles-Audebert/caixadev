package dev.caixadev.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.caixadev.application.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Utilisateur authentifié mais sans autorisation suffisante : renvoie 403.
 * La distinction 401 / 403 est ainsi portée par Spring Security :
 * anonyme → 401 (entry point), authentifié sans droit → 403 (ce handler).
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiErrorResponse.of("FORBIDDEN", "Accès refusé")
        );
    }
}
