package model;

public class OrderItem {
    private int id;
    private int orderId;
    private int productId;
    private int vendorId;
    private String productName;
    private double price;
    private int quantity;
    private double subtotal;
    private String itemStatus;
    private String vendorName;
    private String productImage;

    public OrderItem() {}

    public OrderItem(int productId, String productName, double unitPrice, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.price = unitPrice;
        this.quantity = quantity;
        this.subtotal = unitPrice * quantity;
    }

    public OrderItem(int id, int orderId, int productId, int vendorId, String productName,
                     double price, int quantity, double subtotal, String itemStatus) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.vendorId = vendorId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.subtotal = subtotal;
        this.itemStatus = itemStatus;
    }

    public int getId() { return id; }
    public int getOrderId() { return orderId; }
    public int getProductId() { return productId; }
    public int getVendorId() { return vendorId; }
    public String getProductName() { return productName; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public double getSubtotal() { return subtotal; }
    public String getItemStatus() { return itemStatus; }
    public String getVendorName() { return vendorName; }
    public String getProductImage() { return productImage; }

    public void setId(int id) { this.id = id; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public void setProductId(int productId) { this.productId = productId; }
    public void setVendorId(int vendorId) { this.vendorId = vendorId; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setPrice(double price) { this.price = price; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    public void setItemStatus(String itemStatus) { this.itemStatus = itemStatus; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    public void setProductImage(String productImage) { this.productImage = productImage; }

    public double getTotalPrice() {
        return price * quantity;
    }

    @Override
    public String toString() {
        return "OrderItem{productId=%d, name='%s', price=%.2f, qty=%d, subtotal=%.2f}"
                .formatted(productId, productName, price, quantity, subtotal);
    }
}
