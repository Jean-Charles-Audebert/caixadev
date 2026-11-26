package dev.caixadev.infrastructure.security;

import dev.caixadev.domain.model.ApiKey;
import dev.caixadev.domain.model.ApiKeyVerification;
import dev.caixadev.domain.port.in.ApiKeyUseCase;
import dev.caixadev.infrastructure.web.ClientInfo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authentification des clients machine par clé API ({@code Authorization: Bearer cxd_…}).
 *
 * <p>Utilisé uniquement par la chaîne de sécurité de l'API machine
 * ({@code /cvc-gaines/**}). Volontairement <b>pas</b> un {@code @Component} :
 * Spring Boot l'enregistrerait sinon comme filtre de servlet global, actif
 * sur toutes les URL, y compris celles de l'application à cookie.
 *
 * <p>Une clé absente ou refusée laisse la requête anonyme : la règle
 * d'autorisation de la chaîne produit alors le 401 via
 * {@link RestAuthenticationEntryPoint}. Les refus sont journalisés avec le
 * préfixe (si la clé est bien formée) et l'IP, jamais avec la clé.
 */
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final ApiKeyUseCase apiKeyUseCase;

    public ApiKeyAuthenticationFilter(ApiKeyUseCase apiKeyUseCase) {
        this.apiKeyUseCase = apiKeyUseCase;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null) {
            authenticate(header, request);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String header, HttpServletRequest request) {
        String rawKey = header.regionMatches(true, 0, BEARER, 0, BEARER.length())
                ? header.substring(BEARER.length()).trim()
                : null;

        ApiKeyVerification verification = rawKey == null
                ? ApiKeyVerification.failure(ApiKeyVerification.Status.MALFORMED, null)
                : apiKeyUseCase.authenticate(rawKey);

        if (!verification.isValid()) {
            log.warn("Clé API refusée : motif={}, prefixe={}, ip={}",
                    verification.status(),
                    verification.keyPrefix() != null ? verification.keyPrefix() : "-",
                    ClientInfo.ip(request));
            return;
        }

        ApiKey apiKey = verification.apiKey();
        List<SimpleGrantedAuthority> authorities = apiKey.scopes().stream()
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                .toList();
        var principal = new ApiKeyPrincipal(apiKey.id(), apiKey.name(), apiKey.keyPrefix(), apiKey.scopes());
        var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}
