package service;

import db.Database;
import model.Brand;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BrandService {
    private static volatile List<Brand> cachedBrands = null;

    public static void clearCache() {
        cachedBrands = null;
    }

    public int addBrand(Brand brand) {
        String sql = "INSERT INTO brands (name, description, logo, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, brand.getName());
            stmt.setString(2, brand.getDescription());
            stmt.setString(3, brand.getLogo());
            stmt.setString(4, brand.getStatus() != null ? brand.getStatus() : "ACTIVE");
            stmt.executeUpdate();
            clearCache();
            try (ResultSet keys = stmt.getGeneratedKeys()) { keys.next(); return keys.getInt(1); }
        } catch (SQLException e) { throw new RuntimeException("Failed to add brand: " + e.getMessage(), e); }
    }

    public List<Brand> getAllBrands() {
        List<Brand> cached = cachedBrands;
        if (cached != null) return cached;
        List<Brand> brands = new ArrayList<>();
        String sql = "SELECT * FROM brands WHERE status = 'ACTIVE' ORDER BY name";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) brands.add(mapBrand(rs));
            cachedBrands = Collections.unmodifiableList(brands);
        } catch (SQLException e) { throw new RuntimeException("Failed to fetch brands: " + e.getMessage(), e); }
        return cachedBrands;
    }

    public Brand getBrandById(int id) {
        List<Brand> all = getAllBrands();
        for (Brand b : all) {
            if (b.getId() == id) return b;
        }
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
            boolean ok = stmt.executeUpdate() > 0;
            if (ok) clearCache();
            return ok;
        } catch (SQLException e) { throw new RuntimeException("Failed to update brand: " + e.getMessage(), e); }
    }

    public boolean deleteBrand(int id) {
        String sql = "DELETE FROM brands WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            boolean ok = stmt.executeUpdate() > 0;
            if (ok) clearCache();
            return ok;
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

