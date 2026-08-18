package service;

import db.Database;
import model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryService {

    public int addCategory(Category category) {
        String sql = "INSERT INTO categories (name, description, image, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getDescription());
            stmt.setString(3, category.getImage());
            stmt.setString(4, category.getStatus() != null ? category.getStatus() : "ACTIVE");
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) { keys.next(); return keys.getInt(1); }
        } catch (SQLException e) { throw new RuntimeException("Failed to add category: " + e.getMessage(), e); }
    }

    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM categories WHERE status = 'ACTIVE' ORDER BY name";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) categories.add(mapCategory(rs));
        } catch (SQLException e) { throw new RuntimeException("Failed to fetch categories: " + e.getMessage(), e); }
        return categories;
    }

    public Category getCategoryById(int id) {
        String sql = "SELECT * FROM categories WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) { if (rs.next()) return mapCategory(rs); }
        } catch (SQLException e) { throw new RuntimeException("Failed to fetch category: " + e.getMessage(), e); }
        return null;
    }

    public boolean updateCategory(int id, Category category) {
        String sql = "UPDATE categories SET name = COALESCE(?, name), description = COALESCE(?, description), image = COALESCE(?, image), status = COALESCE(?, status) WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getDescription());
            stmt.setString(3, category.getImage());
            stmt.setString(4, category.getStatus());
            stmt.setInt(5, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException("Failed to update category: " + e.getMessage(), e); }
    }

    public boolean deleteCategory(int id) {
        String sql = "DELETE FROM categories WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException("Failed to delete category: " + e.getMessage(), e); }
    }

    private Category mapCategory(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        c.setImage(rs.getString("image"));
        c.setStatus(rs.getString("status"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        return c;
    }
}
