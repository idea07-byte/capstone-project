package model;

import java.time.LocalDateTime;
import java.util.List;

public class Order {
    private final int id;
    private final Customer customer;
    private final List<OrderItem> items;
    private final LocalDateTime createdAt;

    public Order(int id, Customer customer, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item.");
        }
        this.id = id;
        this.customer = customer;
        this.items = List.copyOf(items);
        this.createdAt = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public double getTotalAmount() {
        return items.stream().mapToDouble(OrderItem::getTotalPrice).sum();
    }

    @Override
    public String toString() {
        return "Order{id=%d, customer=%s, items=%d, total=%.2f, createdAt=%s}"
                .formatted(id, customer.getName(), items.size(), getTotalAmount(), createdAt);
    }
}
