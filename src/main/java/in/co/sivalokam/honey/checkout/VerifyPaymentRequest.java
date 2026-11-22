package in.co.sivalokam.honey.checkout;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VerifyPaymentRequest(
        @NotNull(message = "Order is required")
        UUID orderId,

        @NotBlank(message = "Razorpay order id is required")
        String razorpayOrderId,

        @NotBlank(message = "Razorpay payment id is required")
        String razorpayPaymentId,

        @NotBlank(message = "Razorpay signature is required")
        String razorpaySignature
) {}
