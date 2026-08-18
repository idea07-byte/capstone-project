package model;

import java.sql.Timestamp;

public class Product {
    private int id;
    private int vendorId;
    private Integer categoryId;
    private Integer brandId;
    private String name;
    private String description;
    private double price;
    private double discount;
    private int stockQuantity;
    private String sku;
    private String image;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String vendorName;
    private String categoryName;
    private String brandName;
    private double averageRating;
    private int reviewCount;

    public Product() {}

    public Product(int id, String name, double price, int quantity) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stockQuantity = quantity;
    }

    public Product(int id, int vendorId, Integer categoryId, Integer brandId, String name, String description,
                   double price, double discount, int stockQuantity, String sku, String image, String status) {
        this.id = id;
        this.vendorId = vendorId;
        this.categoryId = categoryId;
        this.brandId = brandId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.discount = discount;
        this.stockQuantity = stockQuantity;
        this.sku = sku;
        this.image = image;
        this.status = status;
    }

    public double getFinalPrice() {
        return discount > 0 ? price - (price * discount / 100) : price;
    }

    public int getId() { return id; }
    public int getVendorId() { return vendorId; }
    public Integer getCategoryId() { return categoryId; }
    public Integer getBrandId() { return brandId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public double getDiscount() { return discount; }
    public int getStockQuantity() { return stockQuantity; }
    public String getSku() { return sku; }
    public String getImage() { return image; }
    public String getStatus() { return status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public String getVendorName() { return vendorName; }
    public String getCategoryName() { return categoryName; }
    public String getBrandName() { return brandName; }
    public double getAverageRating() { return averageRating; }
    public int getReviewCount() { return reviewCount; }

    public void setId(int id) { this.id = id; }
    public void setVendorId(int vendorId) { this.vendorId = vendorId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public void setBrandId(Integer brandId) { this.brandId = brandId; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(double price) { this.price = price; }
    public void setDiscount(double discount) { this.discount = discount; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public void setSku(String sku) { this.sku = sku; }
    public void setImage(String image) { this.image = image; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public void reduceQuantity(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Quantity reduction must be positive.");
        }
        if (amount > stockQuantity) {
            throw new IllegalArgumentException("Not enough stock to reduce by " + amount);
        }
        stockQuantity -= amount;
    }

    @Override
    public String toString() {
        return "Product{id=%d, name='%s', price=%.2f, discount=%.1f%%, stock=%d}".formatted(id, name, price, discount, stockQuantity);
    }
}
