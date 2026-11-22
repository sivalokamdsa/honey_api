package in.co.sivalokam.honey.products;

public record ProductResponse(
        String id,
        String name,
        String description,
        int pricePaise,
        int mrpPaise,
        boolean inStock) {

    static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(),
                p.getPricePaise(), p.getMrpPaise(), p.isInStock());
    }
}
