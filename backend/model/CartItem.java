package model;

public class CartItem {
    private int id;
    private int cartId;
    private int productId;
    private int quantity;
    private double price;
    private String productName;
    private String productImage;
    private int stockQuantity;
    private int vendorId;
    private String vendorName;

    public CartItem() {}

    public CartItem(int id, int cartId, int productId, int quantity, double price) {
        this.id = id;
        this.cartId = cartId;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    public int getId() { return id; }
    public int getCartId() { return cartId; }
    public int getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public String getProductName() { return productName; }
    public String getProductImage() { return productImage; }
    public int getStockQuantity() { return stockQuantity; }
    public int getVendorId() { return vendorId; }
    public String getVendorName() { return vendorName; }

    public void setId(int id) { this.id = id; }
    public void setCartId(int cartId) { this.cartId = cartId; }
    public void setProductId(int productId) { this.productId = productId; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setPrice(double price) { this.price = price; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public void setVendorId(int vendorId) { this.vendorId = vendorId; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public double getSubtotal() {
        return price * quantity;
    }
}
