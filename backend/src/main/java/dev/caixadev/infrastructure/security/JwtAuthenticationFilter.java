package dev.caixadev.infrastructure.security;

import dev.caixadev.domain.exception.InvalidTokenException;
import dev.caixadev.domain.model.TokenClaims;
import dev.caixadev.domain.port.out.TokenPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenPort tokenPort;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractTokenFromCookie(request);

        if (token != null) {
            try {
                TokenClaims claims = tokenPort.parseAccessToken(token);

                List<SimpleGrantedAuthority> authorities = claims.isSuperAdmin()
                        ? List.of(new SimpleGrantedAuthority("ROLE_USER"),
                                  new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                        : List.of(new SimpleGrantedAuthority("ROLE_USER"));

                var auth = new UsernamePasswordAuthenticationToken(claims, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (InvalidTokenException e) {
                log.debug("Token ignoré : {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "access_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}

