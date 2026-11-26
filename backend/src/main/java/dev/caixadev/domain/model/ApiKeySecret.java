package dev.caixadev.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Format et empreinte des clés API : {@code cxd_<préfixe>_<secret>}.
 *
 * <ul>
 *   <li>préfixe : 8 caractères base62, unique en base, sert à retrouver la clé ;</li>
 *   <li>secret : 44 caractères base62 tirés de {@link SecureRandom}, soit
 *       44 × log2(62) ≈ 262 bits d'entropie (au moins 256 bits exigés).</li>
 * </ul>
 * Seule l'empreinte SHA-256 (hex) de la clé complète est conservée.
 */
public final class ApiKeySecret {

    public static final String PREFIX = "cxd_";
    public static final int PREFIX_LENGTH = 8;
    public static final int SECRET_LENGTH = 44;

    private static final String BASE62 =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final Pattern FORMAT = Pattern.compile(
            "^cxd_([0-9A-Za-z]{" + PREFIX_LENGTH + "})_([0-9A-Za-z]{" + SECRET_LENGTH + "})$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private ApiKeySecret() {
    }

    /** Clé générée : préfixe public et clé complète (à ne restituer qu'une fois). */
    public record Generated(String keyPrefix, String rawKey) {
        @Override
        public String toString() {
            // Évite toute fuite accidentelle de la clé via un journal.
            return "Generated[keyPrefix=" + keyPrefix + "]";
        }
    }

    public static Generated generate() {
        String prefix = randomBase62(PREFIX_LENGTH);
        return new Generated(prefix, PREFIX + prefix + "_" + randomBase62(SECRET_LENGTH));
    }

    /** Préfixe d'une clé bien formée, vide sinon (aucune partie du secret n'est renvoyée). */
    public static Optional<String> parsePrefix(String rawKey) {
        if (rawKey == null) {
            return Optional.empty();
        }
        Matcher matcher = FORMAT.matcher(rawKey);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    /** Empreinte SHA-256 hexadécimale (64 caractères) de la clé complète. */
    public static String hash(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawKey.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 non disponible", e);
        }
    }

    /** Comparaison en temps constant de l'empreinte attendue et de celle de la clé présentée. */
    public static boolean hashMatches(String expectedHash, String rawKey) {
        if (expectedHash == null) {
            return false;
        }
        byte[] expected = expectedHash.trim().getBytes(StandardCharsets.US_ASCII);
        byte[] actual = hash(rawKey).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, actual);
    }

    private static String randomBase62(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(BASE62.charAt(RANDOM.nextInt(BASE62.length())));
        }
        return builder.toString();
    }
}
