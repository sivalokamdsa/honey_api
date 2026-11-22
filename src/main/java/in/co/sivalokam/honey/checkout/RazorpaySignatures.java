package in.co.sivalokam.honey.checkout;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

/** HMAC-SHA256 checks for Razorpay payment signatures and webhook signatures. */
public final class RazorpaySignatures {

    private RazorpaySignatures() {
    }

    /** HMAC_SHA256(order_id + "|" + payment_id, keySecret), compared in constant time. */
    public static boolean isValidPaymentSignature(String razorpayOrderId, String razorpayPaymentId,
                                                  String providedSignature, String keySecret) {
        return matches(razorpayOrderId + "|" + razorpayPaymentId, providedSignature, keySecret);
    }

    /** HMAC_SHA256(raw request body, webhookSecret), compared in constant time. */
    public static boolean isValidWebhookSignature(String rawBody, String providedSignature, String webhookSecret) {
        return matches(rawBody, providedSignature, webhookSecret);
    }

    private static boolean matches(String payload, String providedHex, String secret) {
        if (providedHex == null || secret == null || secret.isBlank()) {
            return false;
        }
        byte[] expected = hmacSha256Hex(payload, secret).getBytes(StandardCharsets.UTF_8);
        byte[] actual = providedHex.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    static String hmacSha256Hex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }
}
