package dev.caixadev.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.caixadev.application.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Requête non authentifiée : renvoie 401 avec le format d'erreur de l'API.
 * Sans ce composant, Spring Security répond 403 (Http403ForbiddenEntryPoint),
 * ce qui empêche le frontend de déclencher son renouvellement de session.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiErrorResponse.of("UNAUTHORIZED", "Authentification requise")
        );
    }
}
