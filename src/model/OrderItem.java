package model;

public class OrderItem {
    private final int productId;
    private final String productName;
    private final double unitPrice;
    private final int quantity;

    public OrderItem(int productId, String productName, double unitPrice, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getTotalPrice() {
        return unitPrice * quantity;
    }

    @Override
    public String toString() {
        return "OrderItem{productId=%d, name='%s', unitPrice=%.2f, quantity=%d, total=%.2f}"
                .formatted(productId, productName, unitPrice, quantity, getTotalPrice());
    }
}
