package model;

public class Product {
    private final int id;
    private final String name;
    private final double price;
    private int quantity;

    public Product(int id, String name, double price, int quantity) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void reduceQuantity(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Quantity reduction must be positive.");
        }
        if (amount > quantity) {
            throw new IllegalArgumentException("Not enough stock to reduce by " + amount);
        }
        quantity -= amount;
    }

    @Override
    public String toString() {
        return "Product{id=%d, name='%s', price=%.2f, quantity=%d}".formatted(id, name, price, quantity);
    }
}
