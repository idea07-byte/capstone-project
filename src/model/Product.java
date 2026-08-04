package model;

public class Product {
    private final int id;
    private String name;
    private double price;
    private int quantity;
    private int sellerId;
    private String category;
    private String description;

    public Product(int id, String name, double price, int quantity, int sellerId, String category, String description) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.sellerId = sellerId;
        this.category = category;
        this.description = description;
    }

    public Product(int id, String name, double price, int quantity) {
        this(id, name, price, quantity, 0, "General", "");
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public int getSellerId() { return sellerId; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }

    public void setName(String name) { this.name = name; }
    public void setPrice(double price) { this.price = price; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }
    public void setCategory(String category) { this.category = category; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return "Product{id=%d, name='%s', price=%.2f, quantity=%d, category='%s'}".formatted(id, name, price, quantity, category);
    }
}
