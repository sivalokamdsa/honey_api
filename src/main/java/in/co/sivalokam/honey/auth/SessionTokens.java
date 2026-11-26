package in.co.sivalokam.honey.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

public final class SessionTokens {

    private static final String BEARER = "Bearer ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private SessionTokens() {
    }

    /** 256 bits of randomness, URL-safe. Returned to the client once, at login. */
    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 in hex (64 chars). This is what gets stored, so a leaked table cannot be replayed. */
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /** Extracts the token from "Authorization: Bearer &lt;token&gt;". Empty if absent or malformed. */
    public static Optional<String> fromAuthorizationHeader(String header) {
        if (header == null || !header.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            return Optional.empty();
        }
        String token = header.substring(BEARER.length()).strip();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
