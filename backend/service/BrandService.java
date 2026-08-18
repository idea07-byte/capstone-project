package service;

import db.Database;
import model.Brand;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BrandService {

    public int addBrand(Brand brand) {
        String sql = "INSERT INTO brands (name, description, logo, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, brand.getName());
            stmt.setString(2, brand.getDescription());
            stmt.setString(3, brand.getLogo());
            stmt.setString(4, brand.getStatus() != null ? brand.getStatus() : "ACTIVE");
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) { keys.next(); return keys.getInt(1); }
        } catch (SQLException e) { throw new RuntimeException("Failed to add brand: " + e.getMessage(), e); }
    }

    public List<Brand> getAllBrands() {
        List<Brand> brands = new ArrayList<>();
        String sql = "SELECT * FROM brands WHERE status = 'ACTIVE' ORDER BY name";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) brands.add(mapBrand(rs));
        } catch (SQLException e) { throw new RuntimeException("Failed to fetch brands: " + e.getMessage(), e); }
        return brands;
    }

    public Brand getBrandById(int id) {
        String sql = "SELECT * FROM brands WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) { if (rs.next()) return mapBrand(rs); }
        } catch (SQLException e) { throw new RuntimeException("Failed to fetch brand: " + e.getMessage(), e); }
        return null;
    }

    public boolean updateBrand(int id, Brand brand) {
        String sql = "UPDATE brands SET name = COALESCE(?, name), description = COALESCE(?, description), logo = COALESCE(?, logo), status = COALESCE(?, status) WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, brand.getName());
            stmt.setString(2, brand.getDescription());
            stmt.setString(3, brand.getLogo());
            stmt.setString(4, brand.getStatus());
            stmt.setInt(5, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException("Failed to update brand: " + e.getMessage(), e); }
    }

    public boolean deleteBrand(int id) {
        String sql = "DELETE FROM brands WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException("Failed to delete brand: " + e.getMessage(), e); }
    }

    private Brand mapBrand(ResultSet rs) throws SQLException {
        Brand b = new Brand();
        b.setId(rs.getInt("id"));
        b.setName(rs.getString("name"));
        b.setDescription(rs.getString("description"));
        b.setLogo(rs.getString("logo"));
        b.setStatus(rs.getString("status"));
        b.setCreatedAt(rs.getTimestamp("created_at"));
        return b;
    }
}
