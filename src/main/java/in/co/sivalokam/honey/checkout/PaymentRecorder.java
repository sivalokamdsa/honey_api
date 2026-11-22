package in.co.sivalokam.honey.checkout;

import in.co.sivalokam.honey.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Records a captured payment and marks its order PAID. Shared by the verify endpoint and the
 * webhook. Idempotent: recording the same Razorpay payment twice is a no-op.
 */
@Service
public class PaymentRecorder {

    private final StoreOrderRepository orders;
    private final PaymentRepository payments;
    private final Clock clock;

    public PaymentRecorder(StoreOrderRepository orders, PaymentRepository payments, Clock clock) {
        this.orders = orders;
        this.payments = payments;
        this.clock = clock;
    }

    @Transactional
    public void recordPaid(UUID orderId, String razorpayPaymentId, int amountPaise,
                           PaymentSource source, String signature) {
        // Lock the order row so concurrent verify/webhook calls for it are serialized.
        StoreOrder order = orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        Optional<Payment> existing = payments.findByRazorpayPaymentId(razorpayPaymentId);
        if (existing.isPresent()) {
            if (existing.get().getOrder().getId().equals(orderId)) {
                return; // already recorded: idempotent success
            }
            throw new ApiException(HttpStatus.CONFLICT, "This payment belongs to a different order");
        }

        if (order.getStatus() == OrderStatus.PAID) {
            throw new ApiException(HttpStatus.CONFLICT, "This order has already been paid");
        }
        if (amountPaise != order.getTotalPaise()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Payment amount does not match the order total");
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        payments.save(new Payment(razorpayPaymentId, order, amountPaise, source, signature, now));
        order.markPaid(now);
    }
}
