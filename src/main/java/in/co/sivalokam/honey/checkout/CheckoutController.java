package in.co.sivalokam.honey.checkout;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {

    private final CheckoutService service;

    public CheckoutController(CheckoutService service) {
        this.service = service;
    }

    @PostMapping("/orders")
    public ResponseEntity<CheckoutOrderResponse> createOrder(@Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createOrder(request));
    }

    @PostMapping("/verify")
    public VerifyPaymentResponse verify(@Valid @RequestBody VerifyPaymentRequest request) {
        return service.verifyPayment(request);
    }
}
