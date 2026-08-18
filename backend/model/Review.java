package model;

import java.sql.Timestamp;

public class Review {
    private int id;
    private int productId;
    private int customerId;
    private int rating;
    private String comment;
    private String status;
    private Timestamp createdAt;
    private String customerName;

    public Review() {}

    public Review(int id, int productId, int customerId, int rating, String comment, String status) {
        this.id = id;
        this.productId = productId;
        this.customerId = customerId;
        this.rating = rating;
        this.comment = comment;
        this.status = status;
    }

    public int getId() { return id; }
    public int getProductId() { return productId; }
    public int getCustomerId() { return customerId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public String getStatus() { return status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public String getCustomerName() { return customerName; }

    public void setId(int id) { this.id = id; }
    public void setProductId(int productId) { this.productId = productId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public void setRating(int rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
}
