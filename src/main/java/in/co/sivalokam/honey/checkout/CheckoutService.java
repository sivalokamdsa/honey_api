package in.co.sivalokam.honey.checkout;

import in.co.sivalokam.honey.common.ApiException;
import in.co.sivalokam.honey.common.PhoneNumbers;
import in.co.sivalokam.honey.products.Product;
import in.co.sivalokam.honey.products.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
    private static final String CURRENCY = "INR";

    private final ProductRepository products;
    private final StoreOrderRepository orders;
    private final RazorpayClient razorpay;
    private final PaymentRecorder paymentRecorder;
    private final Clock clock;
    private final String keyId;
    private final String keySecret;

    public CheckoutService(ProductRepository products,
                           StoreOrderRepository orders,
                           RazorpayClient razorpay,
                           PaymentRecorder paymentRecorder,
                           Clock clock,
                           @Value("${razorpay.key-id:}") String keyId,
                           @Value("${razorpay.key-secret:}") String keySecret) {
        this.products = products;
        this.orders = orders;
        this.razorpay = razorpay;
        this.paymentRecorder = paymentRecorder;
        this.clock = clock;
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    /**
     * Prices the cart from the database, checks stock, creates the Razorpay order and stores
     * the internal order. Quantity limits are enforced earlier, by bean validation on the request.
     */
    public CheckoutOrderResponse createOrder(CheckoutRequest req) {
        List<CheckoutRequest.CartItemRequest> items = req.items();
        Map<String, String> errors = new LinkedHashMap<>();

        Set<String> ids = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            if (!ids.add(items.get(i).productId())) {
                errors.putIfAbsent("items[" + i + "].productId", "Each product can appear only once");
            }
        }

        Map<String, Product> byId = new HashMap<>();
        products.findAllById(ids).forEach(p -> byId.put(p.getId(), p));

            for (int i = 0; i < items.size(); i++) {
            String field = "items[" + i + "].productId";
            Product p = byId.get(items.get(i).productId());
            if (p == null) {
                errors.putIfAbsent(field, "Unknown product");
            } else if (!p.isInStock()) {
                errors.putIfAbsent(field, p.getName() + " is out of stock");
            }
        }
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Some items in your cart cannot be ordered", errors);
        }

        List<CheckoutOrderResponse.Line> lines = new ArrayList<>();
        int subtotal = 0;
        for (CheckoutRequest.CartItemRequest item : items) {
            Product p = byId.get(item.productId());
            int quantity = item.quantity();
            int lineTotal = p.getPricePaise() * quantity;
            subtotal += lineTotal;
            lines.add(new CheckoutOrderResponse.Line(p.getId(), p.getName(), p.getPricePaise(), quantity, lineTotal));
        }

        int discount = discountFor(subtotal);
        int total = subtotal - discount;

        UUID orderId = UUID.randomUUID();
        String razorpayOrderId;
        try {
            razorpayOrderId = razorpay.createOrder(total, CURRENCY, orderId.toString());
        } catch (RuntimeException e) {
            log.error("Razorpay order creation failed for internal order {}", orderId, e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "We couldn't start your payment. Please try again.");
        }

        CheckoutRequest.CustomerRequest customer = req.customer();
        StoreOrder order = new StoreOrder(
                orderId,
                customer.name().strip(),
                PhoneNumbers.normalize(customer.phone()),
                blankToNull(customer.email()),
                CURRENCY,
                subtotal,
                discount,
                total,
                razorpayOrderId,
                OffsetDateTime.now(clock));
        for (CheckoutOrderResponse.Line line : lines) {
            order.addItem(new StoreOrderItem(order, line.productId(), line.name(),
                    line.unitPricePaise(), line.quantity()));
        }
        orders.save(order);

        return new CheckoutOrderResponse(orderId, OrderStatus.CREATED.name().toLowerCase(Locale.ROOT),
                razorpayOrderId, keyId, CURRENCY, subtotal, discount, total, lines);
    }

    /** Verifies the Razorpay payment signature and records the payment. Safe to call repeatedly. */
    public VerifyPaymentResponse verifyPayment(VerifyPaymentRequest req) {
        StoreOrder order = orders.findById(req.orderId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getRazorpayOrderId().equals(req.razorpayOrderId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Payment does not belong to this order",
                    Map.of("razorpayOrderId", "Does not match this order"));
        }

        if (!RazorpaySignatures.isValidPaymentSignature(
                req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature(), keySecret)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Payment could not be verified",
                    Map.of("razorpaySignature", "Signature does not match"));
        }

        paymentRecorder.recordPaid(order.getId(), req.razorpayPaymentId(), order.getTotalPaise(),
                PaymentSource.VERIFY, req.razorpaySignature());

        return new VerifyPaymentResponse("paid", order.getId(), req.razorpayPaymentId());
    }

    /**
     * No discount rules are defined yet, so this is always zero. Put coupon or offer logic here.
     */
    private int discountFor(int subtotalPaise) {
        return 0;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.strip().toLowerCase(Locale.ROOT);
    }
}
