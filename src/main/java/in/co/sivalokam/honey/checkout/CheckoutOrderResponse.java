package in.co.sivalokam.honey.checkout;

import java.util.List;
import java.util.UUID;

public record CheckoutOrderResponse(
        UUID orderId,
        String status,
        String razorpayOrderId,
        String razorpayKeyId,
        String currency,
        int subtotalPaise,
        int discountPaise,
        int totalPaise,
        List<Line> items
) {

    public record Line(
            String productId,
            String name,
            int unitPricePaise,
            int quantity,
            int lineTotalPaise
    ) {}
}
