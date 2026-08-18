package service;

import db.Database;
import model.Order;
import model.OrderItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderService {

    public Order createOrder(int customerId, List<OrderItem> items, Integer addressId, String paymentMethod, double discountAmount) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Order must contain at least one item.");

        double totalAmount = items.stream().mapToDouble(OrderItem::getSubtotal).sum();
        double shippingAmount = totalAmount > 500 ? 0 : 49.0;
        double finalAmount = totalAmount + shippingAmount - discountAmount;

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String orderSql = "INSERT INTO orders (customer_id, address_id, total_amount, shipping_amount, discount_amount, final_amount, payment_status, order_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                int orderId;
                try (PreparedStatement stmt = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1, customerId);
                    if (addressId != null) stmt.setInt(2, addressId); else stmt.setNull(2, Types.INTEGER);
                    stmt.setDouble(3, totalAmount);
                    stmt.setDouble(4, shippingAmount);
                    stmt.setDouble(5, discountAmount);
                    stmt.setDouble(6, finalAmount);
                    stmt.setString(7, "PENDING");
                    stmt.setString(8, "PLACED");
                    stmt.executeUpdate();
                    try (ResultSet keys = stmt.getGeneratedKeys()) { keys.next(); orderId = keys.getInt(1); }
                }

                String itemSql = "INSERT INTO order_items (order_id, product_id, vendor_id, product_name, price, quantity, subtotal, item_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(itemSql)) {
                    for (OrderItem item : items) {
                        stmt.setInt(1, orderId);
                        stmt.setInt(2, item.getProductId());
                        stmt.setInt(3, item.getVendorId());
                        stmt.setString(4, item.getProductName());
                        stmt.setDouble(5, item.getPrice());
                        stmt.setInt(6, item.getQuantity());
                        stmt.setDouble(7, item.getSubtotal());
                        stmt.setString(8, "PLACED");
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }

                for (OrderItem item : items) {
                    new ProductService().reduceStock(conn, item.getProductId(), item.getQuantity());
                }

                String paySql = "INSERT INTO payments (order_id, payment_method, transaction_id, amount, payment_status) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(paySql)) {
                    stmt.setInt(1, orderId);
                    stmt.setString(2, paymentMethod != null ? paymentMethod : "COD");
                    stmt.setString(3, "TXN-" + System.currentTimeMillis());
                    stmt.setDouble(4, finalAmount);
                    stmt.setString(5, "COD".equals(paymentMethod) ? "PENDING" : "COMPLETED");
                    stmt.executeUpdate();
                }

                if (paymentMethod != null && !"COD".equals(paymentMethod)) {
                    try (PreparedStatement stmt = conn.prepareStatement("UPDATE orders SET payment_status = 'PAID' WHERE id = ?")) {
                        stmt.setInt(1, orderId);
                        stmt.executeUpdate();
                    }
                }

                conn.commit();
                Order order = new Order(orderId, customerId, totalAmount, "PLACED");
                order.setShippingAmount(shippingAmount);
                order.setDiscountAmount(discountAmount);
                order.setFinalAmount(finalAmount);
                order.setPaymentStatus("COD".equals(paymentMethod) ? "PENDING" : "PAID");
                order.setItems(items);
                return order;
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException("Failed to create order: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create order: " + e.getMessage(), e);
        }
    }

    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.*, u.name as customer_name, u.email as customer_email FROM orders o LEFT JOIN users u ON o.customer_id = u.id ORDER BY o.created_at DESC";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Order order = mapOrder(rs);
                order.setItems(fetchOrderItems(conn, order.getId()));
                orders.add(order);
            }
        } catch (SQLException e) { throw new RuntimeException("Failed to fetch orders: " + e.getMessage(), e); }
        return orders;
    }

    public Order getOrderById(int orderId) {
        String sql = "SELECT o.*, u.name as customer_name, u.email as customer_email FROM orders o LEFT JOIN users u ON o.customer_id = u.id WHERE o.id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Order order = mapOrder(rs);
                    order.setItems(fetchOrderItems(conn, orderId));
                    return order;
                }
            }
        } catch (SQLException e) { throw new RuntimeException("Failed to fetch order: " + e.getMessage(), e); }
        return null;
    }

    public List<Order> getOrdersByCustomerId(int customerId) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.*, u.name as customer_name, u.email as customer_email FROM orders o LEFT JOIN users u ON o.customer_id = u.id WHERE o.customer_id = ? ORDER BY o.created_at DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Order order = mapOrder(rs);
                    order.setItems(fetchOrderItems(conn, order.getId()));
                    orders.add(order);
                }
            }
        } catch (SQLException e) { throw new RuntimeException("Failed to fetch customer orders: " + e.getMessage(), e); }
        return orders;
    }

    public List<Order> getOrdersByVendor(int vendorId) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT DISTINCT o.*, u.name as customer_name, u.email as customer_email FROM orders o INNER JOIN order_items oi ON o.id = oi.order_id LEFT JOIN users u ON o.customer_id = u.id WHERE oi.vendor_id = ? ORDER BY o.created_at DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vendorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Order order = mapOrder(rs);
                    order.setItems(fetchOrderItemsByVendor(conn, order.getId(), vendorId));
                    orders.add(order);
                }
            }
        } catch (SQLException e) { throw new RuntimeException("Failed to fetch vendor orders: " + e.getMessage(), e); }
        return orders;
    }

    public boolean updateOrderStatus(int orderId, String status) {
        String sql = "UPDATE orders SET order_status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, orderId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException("Failed to update order status: " + e.getMessage(), e); }
    }

    public boolean cancelOrder(int orderId) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<OrderItem> items = fetchOrderItems(conn, orderId);
                for (OrderItem item : items) {
                    try (PreparedStatement stmt = conn.prepareStatement("UPDATE products SET stock_quantity = stock_quantity + ? WHERE id = ?")) {
                        stmt.setInt(1, item.getQuantity());
                        stmt.setInt(2, item.getProductId());
                        stmt.executeUpdate();
                    }
                }
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE orders SET order_status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                    stmt.setInt(1, orderId);
                    stmt.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { throw new RuntimeException("Failed to cancel order: " + e.getMessage(), e); }
    }

    public int countAll() {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM orders")) { rs.next(); return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public int countByStatus(String status) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM orders WHERE order_status = ?")) {
            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public int countByVendor(int vendorId) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(DISTINCT o.id) FROM orders o INNER JOIN order_items oi ON o.id = oi.order_id WHERE oi.vendor_id = ?")) {
            stmt.setInt(1, vendorId);
            try (ResultSet rs = stmt.executeQuery()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public double totalRevenue() {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COALESCE(SUM(final_amount), 0) FROM orders WHERE payment_status = 'PAID' OR order_status != 'CANCELLED'")) { rs.next(); return rs.getDouble(1);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public double totalRevenueByVendor(int vendorId) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COALESCE(SUM(oi.subtotal), 0) FROM order_items oi INNER JOIN orders o ON oi.order_id = o.id WHERE oi.vendor_id = ? AND o.order_status != 'CANCELLED'")) {
            stmt.setInt(1, vendorId);
            try (ResultSet rs = stmt.executeQuery()) { rs.next(); return rs.getDouble(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public int nextId(Connection conn, String table) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(id), 0) + 1 FROM " + table)) {
            rs.next(); return rs.getInt(1);
        }
    }

    private List<OrderItem> fetchOrderItems(Connection conn, int orderId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT oi.*, v.business_name as vendor_name FROM order_items oi LEFT JOIN vendors v ON oi.vendor_id = v.id WHERE oi.order_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) items.add(mapOrderItem(rs));
            }
        }
        return items;
    }

    private List<OrderItem> fetchOrderItemsByVendor(Connection conn, int orderId, int vendorId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT oi.*, v.business_name as vendor_name FROM order_items oi LEFT JOIN vendors v ON oi.vendor_id = v.id WHERE oi.order_id = ? AND oi.vendor_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            stmt.setInt(2, vendorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) items.add(mapOrderItem(rs));
            }
        }
        return items;
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getInt("id"));
        order.setCustomerId(rs.getInt("customer_id"));
        try { order.setAddressId(rs.getInt("address_id")); } catch (SQLException e) {}
        order.setTotalAmount(rs.getDouble("total_amount"));
        order.setShippingAmount(rs.getDouble("shipping_amount"));
        order.setDiscountAmount(rs.getDouble("discount_amount"));
        order.setFinalAmount(rs.getDouble("final_amount"));
        order.setPaymentStatus(rs.getString("payment_status"));
        order.setOrderStatus(rs.getString("order_status"));
        order.setCreatedAt(rs.getTimestamp("created_at"));
        order.setUpdatedAt(rs.getTimestamp("updated_at"));
        try { order.setCustomerName(rs.getString("customer_name")); } catch (SQLException e) {}
        try { order.setCustomerEmail(rs.getString("customer_email")); } catch (SQLException e) {}
        return order;
    }

    private OrderItem mapOrderItem(ResultSet rs) throws SQLException {
        OrderItem item = new OrderItem();
        item.setId(rs.getInt("id"));
        item.setOrderId(rs.getInt("order_id"));
        item.setProductId(rs.getInt("product_id"));
        item.setVendorId(rs.getInt("vendor_id"));
        item.setProductName(rs.getString("product_name"));
        item.setPrice(rs.getDouble("price"));
        item.setQuantity(rs.getInt("quantity"));
        item.setSubtotal(rs.getDouble("subtotal"));
        item.setItemStatus(rs.getString("item_status"));
        try { item.setVendorName(rs.getString("vendor_name")); } catch (SQLException e) {}
        return item;
    }
}
