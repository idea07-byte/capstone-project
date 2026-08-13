package service;

import db.Database;
import model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductService {
    public void addProduct(Product product) {
        if (getProductById(product.getId()) != null) {
            throw new IllegalArgumentException("Product ID already exists: " + product.getId());
        }

        String sql = "INSERT INTO products (id, name, price, quantity) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, product.getId());
            stmt.setString(2, product.getName());
            stmt.setDouble(3, product.getPrice());
            stmt.setInt(4, product.getQuantity());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add product: " + e.getMessage(), e);
        }
    }

    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT id, name, price, quantity FROM products";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(mapProduct(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch products: " + e.getMessage(), e);
        }
        return products;
    }

    public Product getProductById(int id) {
        String sql = "SELECT id, name, price, quantity FROM products WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapProduct(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch product: " + e.getMessage(), e);
        }
        return null;
    }

    public boolean updateProduct(int id, String name, double price, int quantity) {
        String sql = "UPDATE products SET name = ?, price = ?, quantity = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setDouble(2, price);
            stmt.setInt(3, quantity);
            stmt.setInt(4, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update product: " + e.getMessage(), e);
        }
    }

    public int nextProductId() {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 FROM products";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get next product ID: " + e.getMessage(), e);
        }
    }

    public boolean removeProductById(int id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove product: " + e.getMessage(), e);
        }
    }

    public boolean hasSufficientStock(int productId, int requestQuantity) {
        String sql = "SELECT quantity FROM products WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int quantity = rs.getInt("quantity");
                    return quantity >= requestQuantity;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check stock: " + e.getMessage(), e);
        }
        return false;
    }

    public void reduceProductQuantity(int productId, int amount) {
        String sql = "UPDATE products SET quantity = quantity - ? WHERE id = ? AND quantity >= ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, amount);
            stmt.setInt(2, productId);
            stmt.setInt(3, amount);
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                Product product = getProductById(productId);
                if (product == null) {
                    throw new IllegalArgumentException("Product not found: " + productId);
                }
                throw new IllegalArgumentException("Not enough stock to reduce by " + amount);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to reduce quantity: " + e.getMessage(), e);
        }
    }

    private Product mapProduct(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        double price = rs.getDouble("price");
        int quantity = rs.getInt("quantity");
        return new Product(id, name, price, quantity);
    }
}
