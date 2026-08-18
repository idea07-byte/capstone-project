package service;

import db.Database;
import model.Vendor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VendorService {

    public int registerVendor(Vendor vendor) {
        String sql = "INSERT INTO vendors (user_id, business_name, owner_name, description, address, city, state, pincode, approval_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, vendor.getUserId());
            stmt.setString(2, vendor.getBusinessName());
            stmt.setString(3, vendor.getOwnerName());
            stmt.setString(4, vendor.getDescription());
            stmt.setString(5, vendor.getAddress());
            stmt.setString(6, vendor.getCity());
            stmt.setString(7, vendor.getState());
            stmt.setString(8, vendor.getPincode());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) { keys.next(); return keys.getInt(1); }
        } catch (SQLException e) { throw new RuntimeException("Failed to register vendor: " + e.getMessage(), e); }
    }

    public Vendor getVendorById(int id) {
        String sql = "SELECT v.*, u.email as owner_email, u.phone as owner_phone FROM vendors v LEFT JOIN users u ON v.user_id = u.id WHERE v.id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) { if (rs.next()) return mapVendor(rs); }
        } catch (SQLException e) { throw new RuntimeException("Failed to fetch vendor: " + e.getMessage(), e); }
        return null;
    }

    public Vendor getVendorByUserId(int userId) {
        String sql = "SELECT v.*, u.email as owner_email, u.phone as owner_phone FROM vendors v LEFT JOIN users u ON v.user_id = u.id WHERE v.user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) { if (rs.next()) return mapVendor(rs); }
        } catch (SQLException e) { throw new RuntimeException("Failed to fetch vendor: " + e.getMessage(), e); }
        return null;
    }

    public List<Vendor> getAllVendors() {
        List<Vendor> vendors = new ArrayList<>();
        String sql = "SELECT v.*, u.email as owner_email, u.phone as owner_phone FROM vendors v LEFT JOIN users u ON v.user_id = u.id ORDER BY v.id DESC";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) vendors.add(mapVendor(rs));
        } catch (SQLException e) { throw new RuntimeException("Failed to fetch vendors: " + e.getMessage(), e); }
        return vendors;
    }

    public List<Vendor> getApprovedVendors() {
        List<Vendor> vendors = new ArrayList<>();
        String sql = "SELECT v.*, u.email as owner_email, u.phone as owner_phone FROM vendors v LEFT JOIN users u ON v.user_id = u.id WHERE v.approval_status = 'APPROVED' ORDER BY v.business_name";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) vendors.add(mapVendor(rs));
        } catch (SQLException e) { throw new RuntimeException("Failed to fetch vendors: " + e.getMessage(), e); }
        return vendors;
    }

    public boolean updateApprovalStatus(int vendorId, String status) {
        String sql = "UPDATE vendors SET approval_status = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, vendorId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException("Failed to update vendor status: " + e.getMessage(), e); }
    }

    public boolean updateVendor(int vendorId, Vendor vendor) {
        String sql = "UPDATE vendors SET business_name = COALESCE(?, business_name), description = COALESCE(?, description), address = COALESCE(?, address), city = COALESCE(?, city), state = COALESCE(?, state), pincode = COALESCE(?, pincode) WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, vendor.getBusinessName());
            stmt.setString(2, vendor.getDescription());
            stmt.setString(3, vendor.getAddress());
            stmt.setString(4, vendor.getCity());
            stmt.setString(5, vendor.getState());
            stmt.setString(6, vendor.getPincode());
            stmt.setInt(7, vendorId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException("Failed to update vendor: " + e.getMessage(), e); }
    }

    public int countAll() {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM vendors")) { rs.next(); return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public int countByStatus(String status) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM vendors WHERE approval_status = ?")) {
            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Vendor mapVendor(ResultSet rs) throws SQLException {
        Vendor v = new Vendor();
        v.setId(rs.getInt("id"));
        v.setUserId(rs.getInt("user_id"));
        v.setBusinessName(rs.getString("business_name"));
        v.setOwnerName(rs.getString("owner_name"));
        v.setDescription(rs.getString("description"));
        v.setAddress(rs.getString("address"));
        v.setCity(rs.getString("city"));
        v.setState(rs.getString("state"));
        v.setPincode(rs.getString("pincode"));
        v.setApprovalStatus(rs.getString("approval_status"));
        v.setCreatedAt(rs.getTimestamp("created_at"));
        try { v.setOwnerEmail(rs.getString("owner_email")); } catch (SQLException e) {}
        try { v.setOwnerPhone(rs.getString("owner_phone")); } catch (SQLException e) {}
        return v;
    }
}
