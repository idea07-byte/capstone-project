package service;

import java.util.ArrayList;
import java.util.List;
import model.Customer;
import model.Order;
import model.OrderItem;

public class OrderService {
    private final List<Order> orders = new ArrayList<>();
    private int nextOrderId = 1;

    public Order createOrder(Customer customer, List<OrderItem> items) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer is required to create an order.");
        }
        Order order = new Order(nextOrderId++, customer, items);
        orders.add(order);
        return order;
    }

    public List<Order> getAllOrders() {
        return List.copyOf(orders);
    }

    public Order getOrderById(int orderId) {
        return orders.stream().filter(order -> order.getId() == orderId).findFirst().orElse(null);
    }

    public List<Order> getOrdersByCustomerId(int customerId) {
        return orders.stream()
                .filter(order -> order.getCustomer().getId() == customerId)
                .toList();
    }
}
