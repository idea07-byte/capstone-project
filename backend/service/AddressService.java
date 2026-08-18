package service;

import db.Database;
import model.Address;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AddressService {

    public int addAddress(Address address) {
        String sql = "INSERT INTO addresses (customer_id, full_name, phone, address_line, city, state, pincode, is_default) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, address.getCustomerId());
            stmt.setString(2, address.getFullName());
            stmt.setString(3, address.getPhone());
            stmt.setString(4, address.getAddressLine());
            stmt.setString(5, address.getCity());
            stmt.setString(6, address.getState());
            stmt.setString(7, address.getPincode());
            stmt.setBoolean(8, address.isDefault());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) { keys.next(); return keys.getInt(1); }
        } catch (SQLException e) { throw new RuntimeException("Failed to add address: " + e.getMessage(), e); }
    }

    public List<Address> getAddressesByCustomer(int customerId) {
        List<Address> addresses = new ArrayList<>();
        String sql = "SELECT * FROM addresses WHERE customer_id = ? ORDER BY is_default DESC, id DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) addresses.add(mapAddress(rs));
            }
        } catch (SQLException e) { throw new RuntimeException("Failed to fetch addresses: " + e.getMessage(), e); }
        return addresses;
    }

    public Address getAddressById(int id) {
        String sql = "SELECT * FROM addresses WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) { if (rs.next()) return mapAddress(rs); }
        } catch (SQLException e) { throw new RuntimeException("Failed to fetch address: " + e.getMessage(), e); }
        return null;
    }

    public boolean deleteAddress(int id) {
        String sql = "DELETE FROM addresses WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException("Failed to delete address: " + e.getMessage(), e); }
    }

    private Address mapAddress(ResultSet rs) throws SQLException {
        return new Address(
            rs.getInt("id"), rs.getInt("customer_id"), rs.getString("full_name"),
            rs.getString("phone"), rs.getString("address_line"), rs.getString("city"),
            rs.getString("state"), rs.getString("pincode"), rs.getBoolean("is_default")
        );
    }
}
