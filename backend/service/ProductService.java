package service;

import db.Database;
import model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductService {
    private static volatile List<Product> cachedAllProducts = null;

    public static void clearCache() {
        cachedAllProducts = null;
    }

    public int addProduct(Product product) {
        String sql = "INSERT INTO products (vendor_id, category_id, brand_id, name, description, price, discount, stock_quantity, sku, image, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, product.getVendorId());
            if (product.getCategoryId() != null) stmt.setInt(2, product.getCategoryId()); else stmt.setNull(2, Types.INTEGER);
            if (product.getBrandId() != null) stmt.setInt(3, product.getBrandId()); else stmt.setNull(3, Types.INTEGER);
            stmt.setString(4, product.getName());
            stmt.setString(5, product.getDescription());
            stmt.setDouble(6, product.getPrice());
            stmt.setDouble(7, product.getDiscount());
            stmt.setInt(8, product.getStockQuantity());
            stmt.setString(9, product.getSku());
            stmt.setString(10, product.getImage());
            stmt.setString(11, product.getStatus() != null ? product.getStatus() : "ACTIVE");
            stmt.executeUpdate();
            clearCache();
            int newId = -1;
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) newId = keys.getInt(1);
            }
            if (newId > 0 && product.getImages() != null && !product.getImages().isEmpty()) {
                saveProductImages(conn, newId, product.getImages());
            }
            return newId;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add product: " + e.getMessage(), e);
        }
    }

    public List<Product> getAllProducts() {
        List<Product> cached = cachedAllProducts;
        if (cached != null) return cached;
        List<Product> list = queryProducts("SELECT p.*, v.business_name as vendor_name, c.name as category_name, b.name as brand_name, COALESCE(AVG(r.rating), 0) as avg_rating, COUNT(r.id) as review_count FROM products p LEFT JOIN vendors v ON p.vendor_id = v.id LEFT JOIN categories c ON p.category_id = c.id LEFT JOIN brands b ON p.brand_id = b.id LEFT JOIN reviews r ON p.id = r.product_id AND r.status = 'ACTIVE' WHERE p.status = 'ACTIVE' GROUP BY p.id, v.business_name, c.name, b.name ORDER BY p.id DESC", null);
        cachedAllProducts = java.util.Collections.unmodifiableList(list);
        return cachedAllProducts;
    }

    public List<Product> getProductsByVendor(int vendorId) {
        return queryProducts("SELECT p.*, v.business_name as vendor_name, c.name as category_name, b.name as brand_name, COALESCE(AVG(r.rating), 0) as avg_rating, COUNT(r.id) as review_count FROM products p LEFT JOIN vendors v ON p.vendor_id = v.id LEFT JOIN categories c ON p.category_id = c.id LEFT JOIN brands b ON p.brand_id = b.id LEFT JOIN reviews r ON p.id = r.product_id AND r.status = 'ACTIVE' WHERE p.vendor_id = ? GROUP BY p.id, v.business_name, c.name, b.name ORDER BY p.id DESC", stmt -> stmt.setInt(1, vendorId));
    }

    public Product getProductById(int id) {
        List<Product> list = queryProducts("SELECT p.*, v.business_name as vendor_name, c.name as category_name, b.name as brand_name, COALESCE(AVG(r.rating), 0) as avg_rating, COUNT(r.id) as review_count FROM products p LEFT JOIN vendors v ON p.vendor_id = v.id LEFT JOIN categories c ON p.category_id = c.id LEFT JOIN brands b ON p.brand_id = b.id LEFT JOIN reviews r ON p.id = r.product_id AND r.status = 'ACTIVE' WHERE p.id = ? GROUP BY p.id, v.business_name, c.name, b.name", stmt -> stmt.setInt(1, id));
        if (list.isEmpty()) return null;
        Product p = list.get(0);
        List<String> imgs = getProductImages(id);
        if (p.getImage() != null && !p.getImage().isEmpty() && !imgs.contains(p.getImage())) {
            imgs.add(0, p.getImage());
        }
        p.setImages(imgs);
        return p;
    }

    public List<String> getProductImages(int productId) {
        List<String> images = new ArrayList<>();
        String sql = "SELECT image_url FROM product_images WHERE product_id = ? ORDER BY is_primary DESC, id ASC";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String u = rs.getString("image_url");
                    if (u != null && !u.isEmpty() && !images.contains(u)) {
                        images.add(u);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to get product images: " + e.getMessage());
        }
        return images;
    }

    public void saveProductImages(Connection conn, int productId, List<String> images) {
        if (images == null || images.isEmpty()) return;
        String delSql = "DELETE FROM product_images WHERE product_id = ?";
        String insSql = "INSERT INTO product_images (product_id, image_url, is_primary) VALUES (?, ?, ?)";
        try (PreparedStatement delStmt = conn.prepareStatement(delSql);
             PreparedStatement insStmt = conn.prepareStatement(insSql)) {
            delStmt.setInt(1, productId);
            delStmt.executeUpdate();
            for (int i = 0; i < images.size(); i++) {
                insStmt.setInt(1, productId);
                insStmt.setString(2, images.get(i));
                insStmt.setBoolean(3, i == 0);
                insStmt.addBatch();
            }
            insStmt.executeBatch();
        } catch (SQLException e) {
            System.err.println("Failed to save product images: " + e.getMessage());
        }
    }

    public List<Product> searchProducts(String query, Integer categoryId, Integer brandId, Double minPrice, Double maxPrice, String sortBy) {
        if ((query == null || query.trim().isEmpty()) && categoryId == null && brandId == null && minPrice == null && maxPrice == null && (sortBy == null || sortBy.isEmpty() || "newest".equals(sortBy))) {
            return getAllProducts();
        }

        StringBuilder sql = new StringBuilder("SELECT p.*, v.business_name as vendor_name, c.name as category_name, b.name as brand_name, COALESCE(AVG(r.rating), 0) as avg_rating, COUNT(r.id) as review_count FROM products p LEFT JOIN vendors v ON p.vendor_id = v.id LEFT JOIN categories c ON p.category_id = c.id LEFT JOIN brands b ON p.brand_id = b.id LEFT JOIN reviews r ON p.id = r.product_id AND r.status = 'ACTIVE' WHERE p.status = 'ACTIVE'");

        List<Object> params = new ArrayList<>();
        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (LOWER(p.name) LIKE LOWER(?) OR LOWER(p.description) LIKE LOWER(?) OR LOWER(v.business_name) LIKE LOWER(?) OR LOWER(b.name) LIKE LOWER(?))");
            String q = "%" + query.trim() + "%";
            params.add(q); params.add(q); params.add(q); params.add(q);
        }
        if (categoryId != null) { sql.append(" AND p.category_id = ?"); params.add(categoryId); }
        if (brandId != null) { sql.append(" AND p.brand_id = ?"); params.add(brandId); }
        if (minPrice != null) { sql.append(" AND p.price >= ?"); params.add(minPrice); }
        if (maxPrice != null) { sql.append(" AND p.price <= ?"); params.add(maxPrice); }

        sql.append(" GROUP BY p.id, v.business_name, c.name, b.name");

        if (sortBy != null) {
            sql.append(switch (sortBy) {
                case "price_asc" -> " ORDER BY p.price ASC";
                case "price_desc" -> " ORDER BY p.price DESC";
                case "name_asc" -> " ORDER BY p.name ASC";
                case "name_desc" -> " ORDER BY p.name DESC";
                case "rating" -> " ORDER BY avg_rating DESC, p.id DESC";
                case "newest" -> " ORDER BY p.created_at DESC";
                default -> " ORDER BY p.id DESC";
            });
        } else {
            sql.append(" ORDER BY p.id DESC");
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof String s) stmt.setString(i + 1, s);
                else if (p instanceof Integer n) stmt.setInt(i + 1, n);
                else if (p instanceof Double d) stmt.setDouble(i + 1, d);
            }
            List<Product> products = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) products.add(mapProduct(rs));
            }
            return products;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search products: " + e.getMessage(), e);
        }
    }

    public boolean updateProduct(int id, Product product) {
        String sql = "UPDATE products SET vendor_id = COALESCE(?, vendor_id), category_id = COALESCE(?, category_id), brand_id = COALESCE(?, brand_id), name = COALESCE(?, name), description = COALESCE(?, description), price = COALESCE(?, price), discount = COALESCE(?, discount), stock_quantity = COALESCE(?, stock_quantity), sku = COALESCE(?, sku), image = COALESCE(?, image), status = COALESCE(?, status), updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (product.getVendorId() > 0) stmt.setInt(1, product.getVendorId()); else stmt.setNull(1, Types.INTEGER);
            if (product.getCategoryId() != null) stmt.setInt(2, product.getCategoryId()); else stmt.setNull(2, Types.INTEGER);
            if (product.getBrandId() != null) stmt.setInt(3, product.getBrandId()); else stmt.setNull(3, Types.INTEGER);
            stmt.setString(4, product.getName());
            stmt.setString(5, product.getDescription());
            stmt.setDouble(6, product.getPrice());
            stmt.setDouble(7, product.getDiscount());
            stmt.setInt(8, product.getStockQuantity());
            stmt.setString(9, product.getSku());
            stmt.setString(10, product.getImage());
            stmt.setString(11, product.getStatus());
            stmt.setInt(12, id);
            boolean ok = stmt.executeUpdate() > 0;
            if (ok) {
                if (product.getImages() != null && !product.getImages().isEmpty()) {
                    saveProductImages(conn, id, product.getImages());
                }
                clearCache();
            }
            return ok;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update product: " + e.getMessage(), e);
        }
    }

    public boolean removeProductById(int id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            boolean ok = stmt.executeUpdate() > 0;
            if (ok) clearCache();
            return ok;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove product: " + e.getMessage(), e);
        }
    }

    public boolean hasSufficientStock(int productId, int requestQuantity) {
        String sql = "SELECT stock_quantity FROM products WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("stock_quantity") >= requestQuantity;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check stock: " + e.getMessage(), e);
        }
        return false;
    }

    public void reduceStock(Connection conn, int productId, int amount) throws SQLException {
        String sql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ? AND stock_quantity >= ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, amount);
            stmt.setInt(2, productId);
            stmt.setInt(3, amount);
            int updated = stmt.executeUpdate();
            if (updated == 0) throw new SQLException("Not enough stock for product " + productId);
            clearCache();
        }
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM products";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next(); return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public int countByVendor(int vendorId) {
        String sql = "SELECT COUNT(*) FROM products WHERE vendor_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vendorId);
            try (ResultSet rs = stmt.executeQuery()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<Product> getLowStockProducts(int vendorId, int threshold) {
        return queryProducts("SELECT p.*, v.business_name as vendor_name, c.name as category_name, b.name as brand_name, COALESCE(AVG(r.rating), 0) as avg_rating, COUNT(r.id) as review_count FROM products p LEFT JOIN vendors v ON p.vendor_id = v.id LEFT JOIN categories c ON p.category_id = c.id LEFT JOIN brands b ON p.brand_id = b.id LEFT JOIN reviews r ON p.id = r.product_id AND r.status = 'ACTIVE' WHERE p.vendor_id = ? AND p.stock_quantity <= ? GROUP BY p.id, v.business_name, c.name, b.name ORDER BY p.stock_quantity ASC", stmt -> { stmt.setInt(1, vendorId); stmt.setInt(2, threshold); });
    }

    @FunctionalInterface
    private interface StmtPreparer { void prepare(PreparedStatement stmt) throws SQLException; }

    private List<Product> queryProducts(String sql, StmtPreparer preparer) {
        List<Product> products = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (preparer != null) preparer.prepare(stmt);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) products.add(mapProduct(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query products: " + e.getMessage(), e);
        }
        return products;
    }

    private Product mapProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setVendorId(rs.getInt("vendor_id"));
        try { p.setCategoryId(rs.getInt("category_id")); } catch (SQLException e) { p.setCategoryId(null); }
        try { p.setBrandId(rs.getInt("brand_id")); } catch (SQLException e) { p.setBrandId(null); }
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setPrice(rs.getDouble("price"));
        p.setDiscount(rs.getDouble("discount"));
        p.setStockQuantity(rs.getInt("stock_quantity"));
        p.setSku(rs.getString("sku"));
        p.setImage(rs.getString("image"));
        p.setStatus(rs.getString("status"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setUpdatedAt(rs.getTimestamp("updated_at"));
        try { p.setVendorName(rs.getString("vendor_name")); } catch (SQLException e) {}
        try { p.setCategoryName(rs.getString("category_name")); } catch (SQLException e) {}
        try { p.setBrandName(rs.getString("brand_name")); } catch (SQLException e) {}
        try { p.setAverageRating(Math.round(rs.getDouble("avg_rating") * 10.0) / 10.0); } catch (SQLException ignored) {}
        try { p.setReviewCount(rs.getInt("review_count")); } catch (SQLException ignored) {}
        return p;
    }
}

