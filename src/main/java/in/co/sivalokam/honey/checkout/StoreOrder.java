package in.co.sivalokam.honey.checkout;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class StoreOrder {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    /** The registration that placed the order. Null only for orders created before login was required. */
    @Column(name = "registration_id")
    private UUID registrationId;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 16)
    private String customerPhone;

    @Column(name = "customer_email", length = 254)
    private String customerEmail;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "subtotal_paise", nullable = false)
    private int subtotalPaise;

    @Column(name = "discount_paise", nullable = false)
    private int discountPaise;

    @Column(name = "total_paise", nullable = false)
    private int totalPaise;

    @Column(name = "razorpay_order_id", nullable = false, unique = true, length = 64)
    private String razorpayOrderId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StoreOrderItem> items = new ArrayList<>();

    protected StoreOrder() {
        // required by JPA
    }

    public StoreOrder(UUID id, UUID registrationId, String customerName, String customerPhone, String customerEmail,
                      String currency, int subtotalPaise, int discountPaise, int totalPaise,
                      String razorpayOrderId, OffsetDateTime createdAt) {
        this.id = id;
        this.registrationId = registrationId;
        this.status = OrderStatus.CREATED;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.customerEmail = customerEmail;
        this.currency = currency;
        this.subtotalPaise = subtotalPaise;
        this.discountPaise = discountPaise;
        this.totalPaise = totalPaise;
        this.razorpayOrderId = razorpayOrderId;
        this.createdAt = createdAt;
    }

    public boolean isOwnedBy(UUID registrationId) {
        return this.registrationId != null && this.registrationId.equals(registrationId);
    }

    public void addItem(StoreOrderItem item) {
        items.add(item);
    }

    public void markPaid(OffsetDateTime when) {
        this.status = OrderStatus.PAID;
        this.paidAt = when;
    }

    public UUID getId() { return id; }
    public UUID getRegistrationId() { return registrationId; }
    public OrderStatus getStatus() { return status; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public String getCustomerEmail() { return customerEmail; }
    public String getCurrency() { return currency; }
    public int getSubtotalPaise() { return subtotalPaise; }
    public int getDiscountPaise() { return discountPaise; }
    public int getTotalPaise() { return totalPaise; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getPaidAt() { return paidAt; }
    public List<StoreOrderItem> getItems() { return items; }
}
