package in.co.sivalokam.honey.checkout;

import in.co.sivalokam.honey.common.ApiException;
import in.co.sivalokam.honey.products.Product;
import in.co.sivalokam.honey.products.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CheckoutServiceTest {

    private static final String KEY_SECRET = "test_secret";
    private static final UUID CUSTOMER = UUID.randomUUID();

    private ProductRepository products;
    private StoreOrderRepository orders;
    private RazorpayClient razorpay;
    private PaymentRecorder recorder;
    private CheckoutService service;
    private Product honey;
    private Product soldOut;

    @BeforeEach
    void setUp() {
        products = mock(ProductRepository.class);
        orders = mock(StoreOrderRepository.class);
        razorpay = mock(RazorpayClient.class);
        recorder = mock(PaymentRecorder.class);
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        service = new CheckoutService(products, orders, razorpay, recorder, clock, "rzp_test_key", KEY_SECRET);

        honey = mock(Product.class);
        when(honey.getId()).thenReturn("honey-1l");
        when(honey.getName()).thenReturn("Pure Honey — 1 Litre");
        when(honey.getPricePaise()).thenReturn(45000);
        when(honey.isInStock()).thenReturn(true);

        soldOut = mock(Product.class);
        when(soldOut.getId()).thenReturn("honey-5l");
        when(soldOut.getName()).thenReturn("Pure Honey — 5 Litre");
        when(soldOut.getPricePaise()).thenReturn(200000);
        when(soldOut.isInStock()).thenReturn(false);

        when(products.findAllById(any())).thenReturn(List.of(honey, soldOut));
    }

    private static CheckoutRequest request(List<CheckoutRequest.CartItemRequest> items) {
        return new CheckoutRequest(items,
                new CheckoutRequest.CustomerRequest("Asha Rao", "9876543210", "Asha@Example.com"));
    }

    private static StoreOrder orderOwnedBy(UUID id, UUID owner) {
        return new StoreOrder(id, owner, "Asha Rao", "+919876543210", null, "INR",
                45000, 0, 45000, "order_XYZ", java.time.OffsetDateTime.now());
    }

    @Test
    void pricesFromServerAndCreatesRazorpayOrderInInr() {
        when(razorpay.createOrder(anyInt(), anyString(), anyString())).thenReturn("order_XYZ");

        CheckoutOrderResponse res = service.createOrder(
                request(List.of(new CheckoutRequest.CartItemRequest("honey-1l", 2))), CUSTOMER);

        assertThat(res.subtotalPaise()).isEqualTo(90000);
        assertThat(res.discountPaise()).isZero();
        assertThat(res.totalPaise()).isEqualTo(90000);
        assertThat(res.razorpayOrderId()).isEqualTo("order_XYZ");
        assertThat(res.razorpayKeyId()).isEqualTo("rzp_test_key");
        assertThat(res.currency()).isEqualTo("INR");
        verify(razorpay).createOrder(eq(90000), eq("INR"), eq(res.orderId().toString()));
        verify(orders).save(any(StoreOrder.class));
    }

    @Test
    void rejectsUnknownProduct() {
        assertThatThrownBy(() -> service.createOrder(
                request(List.of(new CheckoutRequest.CartItemRequest("nope", 1))), CUSTOMER))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getErrors()).containsEntry("items[0].productId", "Unknown product");
                });
        verifyNoInteractions(razorpay);
        verify(orders, never()).save(any());
    }

    @Test
    void rejectsOutOfStockProductServerSide() {
        assertThatThrownBy(() -> service.createOrder(
                request(List.of(new CheckoutRequest.CartItemRequest("honey-5l", 1))), CUSTOMER))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.getErrors()).containsEntry("items[0].productId",
                                "Pure Honey — 5 Litre is out of stock"));
        verifyNoInteractions(razorpay);
    }

    @Test
    void rejectsDuplicateProductLines() {
        assertThatThrownBy(() -> service.createOrder(request(List.of(
                new CheckoutRequest.CartItemRequest("honey-1l", 1),
                new CheckoutRequest.CartItemRequest("honey-1l", 1))), CUSTOMER))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.getErrors()).containsKey("items[1].productId"));
    }

    @Test
    void gatewayFailureIsReportedAndNothingSaved() {
        when(razorpay.createOrder(anyInt(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> service.createOrder(
                request(List.of(new CheckoutRequest.CartItemRequest("honey-1l", 1))), CUSTOMER))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
        verify(orders, never()).save(any());
    }

    @Test
    void verifyRejectsUnknownOrder() {
        UUID id = UUID.randomUUID();
        when(orders.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyPayment(
                new VerifyPaymentRequest(id, "order_XYZ", "pay_1", "sig"), CUSTOMER))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void verifyReportsAnotherCustomersOrderAsNotFound() {
        UUID id = UUID.randomUUID();
        when(orders.findById(id)).thenReturn(Optional.of(orderOwnedBy(id, UUID.randomUUID())));
        String sig = RazorpaySignatures.hmacSha256Hex("order_XYZ|pay_1", KEY_SECRET);

        assertThatThrownBy(() -> service.verifyPayment(
                new VerifyPaymentRequest(id, "order_XYZ", "pay_1", sig), CUSTOMER))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(recorder);
    }

    @Test
    void verifyRejectsBadSignatureWithoutRecording() {
        UUID id = UUID.randomUUID();
        when(orders.findById(id)).thenReturn(Optional.of(orderOwnedBy(id, CUSTOMER)));

        assertThatThrownBy(() -> service.verifyPayment(
                new VerifyPaymentRequest(id, "order_XYZ", "pay_1", "deadbeef"), CUSTOMER))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(recorder);
    }

    @Test
    void verifyRecordsPaymentForValidSignature() {
        UUID id = UUID.randomUUID();
        when(orders.findById(id)).thenReturn(Optional.of(orderOwnedBy(id, CUSTOMER)));
        String sig = RazorpaySignatures.hmacSha256Hex("order_XYZ|pay_1", KEY_SECRET);

        VerifyPaymentResponse res = service.verifyPayment(
                new VerifyPaymentRequest(id, "order_XYZ", "pay_1", sig), CUSTOMER);

        assertThat(res).isEqualTo(new VerifyPaymentResponse("paid", id, "pay_1"));
        verify(recorder).recordPaid(id, "pay_1", 45000, PaymentSource.VERIFY, sig);
    }
}
