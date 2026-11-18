package dev.caixadev.infrastructure.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieUtils {

    /** Nom du cookie de refresh token. */
    public static final String REFRESH_COOKIE_NAME = "refresh_token";

    /** Nom du cookie d'access token. */
    public static final String ACCESS_COOKIE_NAME = "access_token";

    /**
     * Chemin commun aux endpoints qui manipulent le refresh token.
     * Il doit être identique à la création et à la suppression, sinon le navigateur
     * conserve un cookie fantôme. `/api/auth` couvre `/refresh` et `/logout`.
     */
    public static final String REFRESH_COOKIE_PATH = "/api/auth";

    /**
     * Ancien chemin d'émission du cookie de refresh (avant la correction P3-05).
     * Un navigateur peut encore le conserver : comme son chemin est plus spécifique,
     * il serait envoyé avant le cookie actuel sur `/api/auth/refresh` et masquerait
     * le jeton valide. Il est donc supprimé à chaque émission et à chaque logout.
     */
    public static final String LEGACY_REFRESH_COOKIE_PATH = "/api/auth/refresh";

    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-days}")
    private int refreshTokenExpirationDays;

    public void setAccessTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = buildCookie(ACCESS_COOKIE_NAME, token, (int) (accessTokenExpirationMs / 1000));
        response.addCookie(cookie);
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = buildCookie(REFRESH_COOKIE_NAME, token, refreshTokenExpirationDays * 24 * 3600);
        cookie.setPath(REFRESH_COOKIE_PATH);
        response.addCookie(cookie);
        // Purge l'ancien cookie émis sur un chemin plus spécifique (voir constante).
        response.addCookie(legacyRefreshTokenCleanup());
    }

    public void clearAllTokenCookies(HttpServletResponse response) {
        response.addCookie(buildCookie(ACCESS_COOKIE_NAME, "", 0));
        response.addCookie(clearRefreshTokenCookie());
        response.addCookie(legacyRefreshTokenCleanup());
    }

    /**
     * Cookie de suppression du refresh token : mêmes attributs et même chemin que
     * {@link #setRefreshTokenCookie}, condition pour que le navigateur le remplace.
     */
    public Cookie clearRefreshTokenCookie() {
        Cookie cookie = buildCookie(REFRESH_COOKIE_NAME, "", 0);
        cookie.setPath(REFRESH_COOKIE_PATH);
        return cookie;
    }

    /**
     * Suppression ciblée de l'ancien cookie de refresh (chemin `/api/auth/refresh`).
     * Ne remplace pas {@link #clearRefreshTokenCookie()} : les deux chemins coexistent
     * le temps que les navigateurs ayant une ancienne session soient nettoyés.
     */
    public Cookie legacyRefreshTokenCleanup() {
        Cookie cookie = buildCookie(REFRESH_COOKIE_NAME, "", 0);
        cookie.setPath(LEGACY_REFRESH_COOKIE_PATH);
        return cookie;
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
