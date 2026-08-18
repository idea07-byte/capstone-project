package service;

import db.Database;
import model.Review;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewService {

    public int addReview(Review review) {
        String sql = "INSERT INTO reviews (product_id, customer_id, rating, comment, status) VALUES (?, ?, ?, ?, 'ACTIVE')";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, review.getProductId());
            stmt.setInt(2, review.getCustomerId());
            stmt.setInt(3, review.getRating());
            stmt.setString(4, review.getComment());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) { keys.next(); return keys.getInt(1); }
        } catch (SQLException e) { throw new RuntimeException("Failed to add review: " + e.getMessage(), e); }
    }

    public List<Review> getReviewsByProduct(int productId) {
        List<Review> reviews = new ArrayList<>();
        String sql = "SELECT r.*, u.name as customer_name FROM reviews r LEFT JOIN users u ON r.customer_id = u.id WHERE r.product_id = ? AND r.status = 'ACTIVE' ORDER BY r.created_at DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Review r = new Review();
                    r.setId(rs.getInt("id"));
                    r.setProductId(rs.getInt("product_id"));
                    r.setCustomerId(rs.getInt("customer_id"));
                    r.setRating(rs.getInt("rating"));
                    r.setComment(rs.getString("comment"));
                    r.setStatus(rs.getString("status"));
                    r.setCreatedAt(rs.getTimestamp("created_at"));
                    try { r.setCustomerName(rs.getString("customer_name")); } catch (SQLException e) {}
                    reviews.add(r);
                }
            }
        } catch (SQLException e) { throw new RuntimeException("Failed to fetch reviews: " + e.getMessage(), e); }
        return reviews;
    }

    public double getAverageRating(int productId) {
        String sql = "SELECT COALESCE(AVG(rating), 0) FROM reviews WHERE product_id = ? AND status = 'ACTIVE'";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) { rs.next(); return rs.getDouble(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public int getReviewCount(int productId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE product_id = ? AND status = 'ACTIVE'";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public boolean hasCustomerReviewed(int customerId, int productId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE customer_id = ? AND product_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            stmt.setInt(2, productId);
            try (ResultSet rs = stmt.executeQuery()) { rs.next(); return rs.getInt(1) > 0; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public boolean deleteReview(int id) {
        String sql = "DELETE FROM reviews WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
}
