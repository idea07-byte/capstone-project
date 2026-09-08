package service;

import db.Database;
import model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WishlistService {

    public int getOrCreateWishlist(int customerId) {
        String selectSql = "SELECT id FROM wishlist WHERE customer_id = ?";
        String insertSql = "INSERT INTO wishlist (customer_id) VALUES (?) ON CONFLICT (customer_id) DO NOTHING";
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
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) return keys.getInt(1);
                }
            }
            // If already created by concurrent request
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setInt(1, customerId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get/create wishlist: " + e.getMessage(), e);
        }
        return -1;
    }

    public List<Product> getWishlistProducts(int customerId) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.*, v.business_name as vendor_name, c.name as category_name, b.name as brand_name, " +
                     "COALESCE(AVG(r.rating), 0) as avg_rating, COUNT(r.id) as review_count " +
                     "FROM wishlist_items wi " +
                     "INNER JOIN wishlist w ON wi.wishlist_id = w.id " +
                     "INNER JOIN products p ON wi.product_id = p.id " +
                     "LEFT JOIN vendors v ON p.vendor_id = v.id " +
                     "LEFT JOIN categories c ON p.category_id = c.id " +
                     "LEFT JOIN brands b ON p.brand_id = b.id " +
                     "LEFT JOIN reviews r ON p.id = r.product_id AND r.status = 'ACTIVE' " +
                     "WHERE w.customer_id = ? AND p.status = 'ACTIVE' " +
                     "GROUP BY wi.id, p.id, v.business_name, c.name, b.name " +
                     "ORDER BY wi.id DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                ProductService ps = new ProductService();
                while (rs.next()) {
                    Product p = ps.mapProduct(rs);
                    products.add(p);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch wishlist products: " + e.getMessage(), e);
        }
        return products;
    }

    public boolean addToWishlist(int customerId, int productId) {
        int wishlistId = getOrCreateWishlist(customerId);
        if (wishlistId <= 0) return false;
        String sql = "INSERT INTO wishlist_items (wishlist_id, product_id) VALUES (?, ?) ON CONFLICT (wishlist_id, product_id) DO NOTHING";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, wishlistId);
            stmt.setInt(2, productId);
            return stmt.executeUpdate() >= 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add to wishlist: " + e.getMessage(), e);
        }
    }

    public boolean removeFromWishlist(int customerId, int productId) {
        String sql = "DELETE FROM wishlist_items WHERE product_id = ? AND wishlist_id = (SELECT id FROM wishlist WHERE customer_id = ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            stmt.setInt(2, customerId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove from wishlist: " + e.getMessage(), e);
        }
    }

    public boolean isWishlisted(int customerId, int productId) {
        String sql = "SELECT 1 FROM wishlist_items wi INNER JOIN wishlist w ON wi.wishlist_id = w.id WHERE w.customer_id = ? AND wi.product_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            stmt.setInt(2, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public int getWishlistCount(int customerId) {
        String sql = "SELECT COUNT(*) FROM wishlist_items wi INNER JOIN wishlist w ON wi.wishlist_id = w.id WHERE w.customer_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            return 0;
        }
        return 0;
    }
}
