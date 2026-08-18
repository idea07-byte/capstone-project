package service;

import db.Database;
import model.CartItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CartService {

    public int getOrCreateCart(int customerId) {
        String selectSql = "SELECT id FROM cart WHERE customer_id = ?";
        String insertSql = "INSERT INTO cart (customer_id) VALUES (?)";
        try (Connection conn = Database.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setInt(1, customerId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) return rs.getInt("id");
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, customerId);
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) { keys.next(); return keys.getInt(1); }
            }
        } catch (SQLException e) { throw new RuntimeException("Failed to get/create cart: " + e.getMessage(), e); }
    }

    public List<CartItem> getCartItems(int customerId) {
        List<CartItem> items = new ArrayList<>();
        String sql = "SELECT ci.*, p.name as product_name, p.image as product_image, p.stock_quantity, p.vendor_id, v.business_name as vendor_name FROM cart_items ci INNER JOIN cart c ON ci.cart_id = c.id INNER JOIN products p ON ci.product_id = p.id LEFT JOIN vendors v ON p.vendor_id = v.id WHERE c.customer_id = ? ORDER BY ci.id";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    CartItem item = new CartItem();
                    item.setId(rs.getInt("id"));
                    item.setCartId(rs.getInt("cart_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setPrice(rs.getDouble("price"));
                    item.setProductName(rs.getString("product_name"));
                    item.setProductImage(rs.getString("product_image"));
                    item.setStockQuantity(rs.getInt("stock_quantity"));
                    item.setVendorId(rs.getInt("vendor_id"));
                    item.setVendorName(rs.getString("vendor_name"));
                    items.add(item);
                }
            }
        } catch (SQLException e) { throw new RuntimeException("Failed to fetch cart items: " + e.getMessage(), e); }
        return items;
    }

    public void addToCart(int customerId, int productId, int quantity, double price) {
        int cartId = getOrCreateCart(customerId);
        String sql = "INSERT INTO cart_items (cart_id, product_id, quantity, price) VALUES (?, ?, ?, ?) ON CONFLICT (cart_id, product_id) DO UPDATE SET quantity = cart_items.quantity + EXCLUDED.quantity";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, cartId);
            stmt.setInt(2, productId);
            stmt.setInt(3, quantity);
            stmt.setDouble(4, price);
            stmt.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("Failed to add to cart: " + e.getMessage(), e); }
    }

    public boolean updateCartItem(int customerId, int productId, int quantity) {
        String sql = "UPDATE ci SET quantity = ? FROM cart_items ci INNER JOIN cart c ON ci.cart_id = c.id WHERE c.customer_id = ? AND ci.product_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE cart_items SET quantity = ? WHERE product_id = ? AND cart_id = (SELECT id FROM cart WHERE customer_id = ?)")) {
            stmt.setInt(1, quantity);
            stmt.setInt(2, productId);
            stmt.setInt(3, customerId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException("Failed to update cart item: " + e.getMessage(), e); }
    }

    public boolean removeFromCart(int customerId, int productId) {
        String sql = "DELETE FROM ci USING cart c WHERE ci.cart_id = c.id AND c.customer_id = ? AND ci.product_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM cart_items WHERE product_id = ? AND cart_id = (SELECT id FROM cart WHERE customer_id = ?)")) {
            stmt.setInt(1, productId);
            stmt.setInt(2, customerId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException("Failed to remove from cart: " + e.getMessage(), e); }
    }

    public void clearCart(int customerId) {
        String sql = "DELETE FROM cart_items WHERE cart_id = (SELECT id FROM cart WHERE customer_id = ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            stmt.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("Failed to clear cart: " + e.getMessage(), e); }
    }

    public int getCartCount(int customerId) {
        String sql = "SELECT COALESCE(SUM(ci.quantity), 0) FROM cart_items ci INNER JOIN cart c ON ci.cart_id = c.id WHERE c.customer_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
}
