package in.co.sivalokam.honey.checkout;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
class RestRazorpayClient implements RazorpayClient {

    private final RestClient http;
    private final String keyId;
    private final String keySecret;

    RestRazorpayClient(RestClient razorpayHttpClient,
                       @Value("${razorpay.key-id:}") String keyId,
                       @Value("${razorpay.key-secret:}") String keySecret) {
        this.http = razorpayHttpClient;
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    @Override
    public String createOrder(int amountPaise, String currency, String receipt) {
        Map<String, Object> body = Map.of(
                "amount", amountPaise,
                "currency", currency,
                "receipt", receipt);

        RazorpayOrderResponse response = http.post()
                .uri("/v1/orders")
                .headers(h -> h.setBasicAuth(keyId, keySecret))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(RazorpayOrderResponse.class);

        if (response == null || response.id() == null || response.id().isBlank()) {
            throw new IllegalStateException("Razorpay returned no order id");
        }
        return response.id();
    }

    private record RazorpayOrderResponse(String id) {}
}
