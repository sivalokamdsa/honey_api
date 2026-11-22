package in.co.sivalokam.honey.checkout;

import java.util.UUID;

public record VerifyPaymentResponse(String status, UUID orderId, String paymentId) {}
