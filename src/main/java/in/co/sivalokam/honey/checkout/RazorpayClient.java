package in.co.sivalokam.honey.checkout;

/** Creates orders on Razorpay. Implementations use the secret key. */
public interface RazorpayClient {

    /** @return the Razorpay order id (e.g. "order_XXXX") */
    String createOrder(int amountPaise, String currency, String receipt);
}
