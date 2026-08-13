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
        if (getUserById(user.getId()) != null) {
            throw new IllegalArgumentException("User ID already exists: " + user.getId());
        }

        String sql = "INSERT INTO users (id, name, email, password, role) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            stmt.setString(2, user.getName());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPassword());
            stmt.setString(5, user.getRole().name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add user: " + e.getMessage(), e);
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
        String sql = "SELECT id, name, email, password, role FROM users";

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

    public List<User> getUsersByRole(Role role) {        List<User> users = new ArrayList<>();
        String sql = "SELECT id, name, email, password, role FROM users WHERE role = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, role.name());
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

    public User getUserById(int id) {
        String sql = "SELECT id, name, email, password, role FROM users WHERE id = ?";
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

    private User mapUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String password = rs.getString("password");
        Role role = Role.valueOf(rs.getString("role"));

        if (role == Role.ADMIN) {
            return new Admin(id, name, email, password);
        } else {
            return new Customer(id, name, email, password);
        }
    }
}
