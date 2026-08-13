package service;

import db.Database;
import model.Customer;
import model.Order;
import model.OrderItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderService {

    public Order createOrder(Customer customer, List<OrderItem> items) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer is required to create an order.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item.");
        }

        int orderId;
        double totalAmount = items.stream().mapToDouble(OrderItem::getTotalPrice).sum();

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                orderId = nextId(conn, "orders");
                String orderSql = "INSERT INTO orders (id, customer_id, total_amount, status) VALUES (?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(orderSql)) {
                    stmt.setInt(1, orderId);
                    stmt.setInt(2, customer.getId());
                    stmt.setDouble(3, totalAmount);
                    stmt.setString(4, "Pending");
                    stmt.executeUpdate();
                }

                String itemSql = "INSERT INTO order_items (id, order_id, product_id, product_name, unit_price, quantity) VALUES (?, ?, ?, ?, ?, ?)";
                int itemIdBase = nextId(conn, "order_items");
                try (PreparedStatement stmt = conn.prepareStatement(itemSql)) {
                    for (int i = 0; i < items.size(); i++) {
                        OrderItem item = items.get(i);
                        stmt.setInt(1, itemIdBase + i);
                        stmt.setInt(2, orderId);
                        stmt.setInt(3, item.getProductId());
                        stmt.setString(4, item.getProductName());
                        stmt.setDouble(5, item.getUnitPrice());
                        stmt.setInt(6, item.getQuantity());
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }

                String reduceSql = "UPDATE products SET quantity = quantity - ? WHERE id = ? AND quantity >= ?";
                try (PreparedStatement stmt = conn.prepareStatement(reduceSql)) {
                    for (OrderItem item : items) {
                        stmt.setInt(1, item.getQuantity());
                        stmt.setInt(2, item.getProductId());
                        stmt.setInt(3, item.getQuantity());
                        int updated = stmt.executeUpdate();
                        if (updated == 0) {
                            throw new SQLException("Not enough stock for product id " + item.getProductId());
                        }
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException("Failed to create order: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create order: " + e.getMessage(), e);
        }

        return new Order(orderId, customer, items);
    }

    private int nextId(Connection conn, String table) throws SQLException {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 FROM " + table;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT id, customer_id, total_amount, status, created_at FROM orders ORDER BY created_at DESC";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int orderId = rs.getInt("id");
                int customerId = rs.getInt("customer_id");
                Customer customer = fetchCustomer(conn, customerId);
                List<OrderItem> items = fetchOrderItems(conn, orderId);
                orders.add(new Order(orderId, customer, items));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch orders: " + e.getMessage(), e);
        }
        return orders;
    }

    public Order getOrderById(int orderId) {
        String sql = "SELECT id, customer_id, total_amount, status, created_at FROM orders WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int customerId = rs.getInt("customer_id");
                    Customer customer = fetchCustomer(conn, customerId);
                    List<OrderItem> items = fetchOrderItems(conn, orderId);
                    return new Order(orderId, customer, items);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch order: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Order> getOrdersByCustomerId(int customerId) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT id, customer_id, total_amount, status, created_at FROM orders WHERE customer_id = ? ORDER BY created_at DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int orderId = rs.getInt("id");
                    Customer customer = fetchCustomer(conn, customerId);
                    List<OrderItem> items = fetchOrderItems(conn, orderId);
                    orders.add(new Order(orderId, customer, items));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch customer orders: " + e.getMessage(), e);
        }
        return orders;
    }

    private Customer fetchCustomer(Connection conn, int customerId) throws SQLException {
        String sql = "SELECT id, name, email, password FROM users WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Customer(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password")
                    );
                }
            }
        }
        throw new SQLException("Customer not found: " + customerId);
    }

    private List<OrderItem> fetchOrderItems(Connection conn, int orderId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT product_id, product_name, unit_price, quantity FROM order_items WHERE order_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new OrderItem(
                        rs.getInt("product_id"),
                        rs.getString("product_name"),
                        rs.getDouble("unit_price"),
                        rs.getInt("quantity")
                    ));
                }
            }
        }
        return items;
    }
}
