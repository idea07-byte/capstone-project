package service;

import db.Database;
import model.Role;
import model.User;
import model.Customer;
import model.Admin;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    public void addUser(User user) {
        String sql = "INSERT INTO users (name, email, phone, password, role, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPhone());
            stmt.setString(4, user.getPassword());
            stmt.setString(5, user.getRole().name());
            stmt.setString(6, user.getStatus() != null ? user.getStatus() : "ACTIVE");
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    setField(user, "id", keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add user: " + e.getMessage(), e);
        }
    }

    private void setField(User user, String field, int value) {
        try {
            java.lang.reflect.Field f = User.class.getDeclaredField(field);
            f.setAccessible(true);
            f.setInt(user, value);
        } catch (Exception e) {
            // ignore for abstract final fields
        }
    }

    public boolean removeUserById(int id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove user: " + e.getMessage(), e);
        }
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, name, email, phone, password, role, status, created_at, updated_at FROM users ORDER BY id";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch users: " + e.getMessage(), e);
        }
        return users;
    }

    public User getUserById(int id) {
        String sql = "SELECT id, name, email, phone, password, role, status, created_at, updated_at FROM users WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch user: " + e.getMessage(), e);
        }
        return null;
    }

    public User getUserByEmail(String email) {
        String sql = "SELECT id, name, email, phone, password, role, status, created_at, updated_at FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch user by email: " + e.getMessage(), e);
        }
        return null;
    }

    public boolean updateUser(int id, String name, String email, String phone, String status) {
        String sql = "UPDATE users SET name = COALESCE(?, name), email = COALESCE(?, email), phone = COALESCE(?, phone), status = COALESCE(?, status), updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, phone);
            stmt.setString(4, status);
            stmt.setInt(5, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user: " + e.getMessage(), e);
        }
    }

    public boolean updateUserStatus(int id, String status) {
        return updateUser(id, null, null, null, status);
    }

    public int countByRole(String role) {
        String sql = "SELECT COUNT(*) FROM users WHERE role = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, role);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count users: " + e.getMessage(), e);
        }
        return 0;
    }

    public List<User> getUsersByRole(String role) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, name, email, phone, password, role, status, created_at, updated_at FROM users WHERE role = ? ORDER BY id";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, role);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch users by role: " + e.getMessage(), e);
        }
        return users;
    }

    public int nextUserId() {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 FROM users";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to compute next user id: " + e.getMessage(), e);
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String phone = rs.getString("phone");
        String password = rs.getString("password");
        Role role = Role.valueOf(rs.getString("role"));
        String status = rs.getString("status");
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");

        return switch (role) {
            case ADMIN -> new Admin(id, name, email, phone, password, status, createdAt, updatedAt);
            case VENDOR -> new model.VendorUser(id, name, email, phone, password, status, createdAt, updatedAt);
            default -> new Customer(id, name, email, phone, password, status, createdAt, updatedAt);
        };
    }
}
