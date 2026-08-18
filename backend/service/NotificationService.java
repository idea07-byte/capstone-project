package service;

import db.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NotificationService {

    public void create(int userId, String title, String message, String type) {
        String sql = "INSERT INTO notifications (user_id, title, message, type) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, title);
            stmt.setString(3, message);
            stmt.setString(4, type);
            stmt.executeUpdate();
        } catch (SQLException e) { System.err.println("Failed to create notification: " + e.getMessage()); }
    }

    public List<Map<String, Object>> getByUser(int userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 50";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(Map.of(
                        "id", rs.getInt("id"),
                        "title", rs.getString("title"),
                        "message", rs.getString("message"),
                        "type", rs.getString("type"),
                        "is_read", rs.getBoolean("is_read"),
                        "created_at", rs.getTimestamp("created_at").toString()
                    ));
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public int getUnreadCount(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { return 0; }
    }

    public void markAsRead(int userId) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE notifications SET is_read = TRUE WHERE user_id = ?")) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) { }
    }
}
