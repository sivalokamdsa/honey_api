package in.co.sivalokam.honey.products;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "price_paise", nullable = false)
    private int pricePaise;

    @Column(name = "mrp_paise", nullable = false)
    private int mrpPaise;

    @Column(name = "in_stock", nullable = false)
    private boolean inStock;

    protected Product() {
        // required by JPA
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getPricePaise() { return pricePaise; }
    public int getMrpPaise() { return mrpPaise; }
    public boolean isInStock() { return inStock; }
}
