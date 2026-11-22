package in.co.sivalokam.honey.checkout;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RazorpaySignaturesTest {

    private static final String SECRET = "test_secret";

    @Test
    void acceptsCorrectPaymentSignature() {
        String sig = RazorpaySignatures.hmacSha256Hex("order_abc|pay_123", SECRET);

        assertThat(RazorpaySignatures.isValidPaymentSignature("order_abc", "pay_123", sig, SECRET)).isTrue();
    }

    @Test
    void rejectsTamperedPaymentId() {
        String sig = RazorpaySignatures.hmacSha256Hex("order_abc|pay_123", SECRET);

        assertThat(RazorpaySignatures.isValidPaymentSignature("order_abc", "pay_999", sig, SECRET)).isFalse();
    }

    @Test
    void rejectsWrongSecret() {
        String sig = RazorpaySignatures.hmacSha256Hex("order_abc|pay_123", "other_secret");

        assertThat(RazorpaySignatures.isValidPaymentSignature("order_abc", "pay_123", sig, SECRET)).isFalse();
    }

    @Test
    void rejectsMissingSignatureOrSecret() {
        assertThat(RazorpaySignatures.isValidPaymentSignature("order_abc", "pay_123", null, SECRET)).isFalse();
        assertThat(RazorpaySignatures.isValidPaymentSignature("order_abc", "pay_123", "abc", "")).isFalse();
    }

    @Test
    void validatesWebhookBodySignature() {
        String body = "{\"event\":\"payment.captured\"}";
        String sig = RazorpaySignatures.hmacSha256Hex(body, SECRET);

        assertThat(RazorpaySignatures.isValidWebhookSignature(body, sig, SECRET)).isTrue();
        assertThat(RazorpaySignatures.isValidWebhookSignature(body + " ", sig, SECRET)).isFalse();
    }
}
