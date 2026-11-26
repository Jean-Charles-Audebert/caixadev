package dev.caixadev.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Informations sur le client destinees aux journaux de securite (T38).
 *
 * <p>En production, seul Cloudflare atteint l'origine (liste d'IP autorisees
 * dans Traefik) : l'en-tete {@code CF-Connecting-IP} porte donc l'adresse
 * reelle du client. A defaut (developpement, tests), on retombe sur
 * {@code X-Forwarded-For} puis sur l'adresse de la connexion.
 */
public final class ClientInfo {

    private ClientInfo() {
    }

    public static String ip(HttpServletRequest request) {
        String cf = request.getHeader("CF-Connecting-IP");
        if (isPresent(cf)) {
            return sanitize(cf);
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (isPresent(xff)) {
            return sanitize(xff.split(",")[0]);
        }
        return request.getRemoteAddr();
    }

    /**
     * Masque une adresse email pour les journaux : premiere lettre et domaine
     * conserves ({@code j***@example.com}). Suffisant pour reperer une
     * attaque ciblee sans ecrire l'identifiant en clair.
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "-";
        }
        String value = sanitize(email);
        int at = value.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return value.charAt(0) + "***" + value.substring(at);
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    /** Empeche l'injection de lignes dans les journaux et borne la longueur. */
    private static String sanitize(String value) {
        String cleaned = value.trim().replaceAll("[\\r\\n\\t]", "_");
        return cleaned.length() > 100 ? cleaned.substring(0, 100) : cleaned;
    }
}
