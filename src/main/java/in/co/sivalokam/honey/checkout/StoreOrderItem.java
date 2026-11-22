package in.co.sivalokam.honey.checkout;

import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class StoreOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private StoreOrder order;

    @Column(name = "product_id", nullable = false, length = 64)
    private String productId;

    @Column(name = "product_name", nullable = false, length = 120)
    private String productName;

    @Column(name = "unit_price_paise", nullable = false)
    private int unitPricePaise;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total_paise", nullable = false)
    private int lineTotalPaise;

    protected StoreOrderItem() {
        // required by JPA
    }

    public StoreOrderItem(StoreOrder order, String productId, String productName,
                          int unitPricePaise, int quantity) {
        this.order = order;
        this.productId = productId;
        this.productName = productName;
        this.unitPricePaise = unitPricePaise;
        this.quantity = quantity;
        this.lineTotalPaise = unitPricePaise * quantity;
    }

    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getUnitPricePaise() { return unitPricePaise; }
    public int getQuantity() { return quantity; }
    public int getLineTotalPaise() { return lineTotalPaise; }
}
