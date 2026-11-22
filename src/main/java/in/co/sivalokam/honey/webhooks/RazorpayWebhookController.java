package in.co.sivalokam.honey.webhooks;

import in.co.sivalokam.honey.checkout.PaymentRecorder;
import in.co.sivalokam.honey.checkout.PaymentSource;
import in.co.sivalokam.honey.checkout.RazorpaySignatures;
import in.co.sivalokam.honey.checkout.StoreOrder;
import in.co.sivalokam.honey.checkout.StoreOrderRepository;
import in.co.sivalokam.honey.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

/**
 * Razorpay webhook. Records payments even when the browser never calls /checkout/verify.
 * Signed with the webhook secret, which is separate from the API key secret.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
public class RazorpayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookController.class);

    private final String webhookSecret;
    private final StoreOrderRepository orders;
    private final PaymentRecorder paymentRecorder;
    private final ObjectMapper mapper;

    public RazorpayWebhookController(@Value("${razorpay.webhook-secret:}") String webhookSecret,
                                     StoreOrderRepository orders,
                                     PaymentRecorder paymentRecorder,
                                     ObjectMapper mapper) {
        this.webhookSecret = webhookSecret;
        this.orders = orders;
        this.paymentRecorder = paymentRecorder;
        this.mapper = mapper;
    }

    @PostMapping("/razorpay")
    public ResponseEntity<Void> handle(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestBody String rawBody) {

        if (!RazorpaySignatures.isValidWebhookSignature(rawBody, signature, webhookSecret)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid webhook signature");
        }

        JsonNode root = mapper.readTree(rawBody);
        if (!"payment.captured".equals(root.path("event").asString())) {
            return ResponseEntity.ok().build(); // other events are acknowledged and ignored
        }

        JsonNode payment = root.path("payload").path("payment").path("entity");
        String razorpayOrderId = payment.path("order_id").asString();
        String razorpayPaymentId = payment.path("id").asString();
        int amountPaise = payment.path("amount").asInt();

        Optional<StoreOrder> order = orders.findByRazorpayOrderId(razorpayOrderId);
        if (order.isEmpty()) {
            log.warn("Webhook for unknown Razorpay order {}", razorpayOrderId);
            return ResponseEntity.ok().build();
        }

        try {
            paymentRecorder.recordPaid(order.get().getId(), razorpayPaymentId, amountPaise,
                    PaymentSource.WEBHOOK, null);
        } catch (ApiException e) {
            // Acknowledge anyway so Razorpay does not retry a payment we cannot apply.
            log.warn("Webhook payment {} not applied: {}", razorpayPaymentId, e.getMessage());
        }
        return ResponseEntity.ok().build();
    }
}
