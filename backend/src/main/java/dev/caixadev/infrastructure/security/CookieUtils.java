package dev.caixadev.infrastructure.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieUtils {

    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-days}")
    private int refreshTokenExpirationDays;

    public void setAccessTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = buildCookie("access_token", token, (int) (accessTokenExpirationMs / 1000));
        response.addCookie(cookie);
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = buildCookie("refresh_token", token, refreshTokenExpirationDays * 24 * 3600);
        cookie.setPath("/api/auth/refresh");
        response.addCookie(cookie);
    }

    public void clearAllTokenCookies(HttpServletResponse response) {
        response.addCookie(buildCookie("access_token", "", 0));
        response.addCookie(buildCookie("refresh_token", "", 0));
    }

    private Cookie buildCookie(String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Strict");
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        return cookie;
    }
}

