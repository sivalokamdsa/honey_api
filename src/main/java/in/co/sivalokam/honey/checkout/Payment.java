package in.co.sivalokam.honey.checkout;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "razorpay_payment_id", nullable = false, unique = true, length = 64)
    private String razorpayPaymentId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private StoreOrder order;

    @Column(name = "amount_paise", nullable = false)
    private int amountPaise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentSource source;

    @Column(name = "razorpay_signature", length = 256)
    private String razorpaySignature;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Payment() {
        // required by JPA
    }

    public Payment(String razorpayPaymentId, StoreOrder order, int amountPaise,
                   PaymentSource source, String razorpaySignature, OffsetDateTime createdAt) {
        this.razorpayPaymentId = razorpayPaymentId;
        this.order = order;
        this.amountPaise = amountPaise;
        this.status = PaymentStatus.PAID;
        this.source = source;
        this.razorpaySignature = razorpaySignature;
        this.createdAt = createdAt;
    }

    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public StoreOrder getOrder() { return order; }
    public int getAmountPaise() { return amountPaise; }
    public PaymentStatus getStatus() { return status; }
    public PaymentSource getSource() { return source; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
