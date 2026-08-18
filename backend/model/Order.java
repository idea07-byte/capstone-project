package model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private int id;
    private int customerId;
    private Integer addressId;
    private double totalAmount;
    private double shippingAmount;
    private double discountAmount;
    private double finalAmount;
    private String paymentStatus;
    private String orderStatus;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String customerName;
    private String customerEmail;
    private List<OrderItem> items;

    public Order() {
        this.items = new ArrayList<>();
    }

    public Order(int id, int customerId, double totalAmount, String orderStatus) {
        this.id = id;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.finalAmount = totalAmount;
        this.orderStatus = orderStatus;
        this.items = new ArrayList<>();
    }

    public Order(int id, int customerId, double totalAmount, double shippingAmount,
                 double discountAmount, double finalAmount, String paymentStatus, String orderStatus) {
        this.id = id;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.shippingAmount = shippingAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.paymentStatus = paymentStatus;
        this.orderStatus = orderStatus;
        this.items = new ArrayList<>();
    }

    public int getId() { return id; }
    public int getCustomerId() { return customerId; }
    public Integer getAddressId() { return addressId; }
    public double getTotalAmount() { return totalAmount; }
    public double getShippingAmount() { return shippingAmount; }
    public double getDiscountAmount() { return discountAmount; }
    public double getFinalAmount() { return finalAmount; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getOrderStatus() { return orderStatus; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public String getCustomerName() { return customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public List<OrderItem> getItems() { return items; }

    public void setId(int id) { this.id = id; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public void setAddressId(Integer addressId) { this.addressId = addressId; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public void setShippingAmount(double shippingAmount) { this.shippingAmount = shippingAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }
    public void setFinalAmount(double finalAmount) { this.finalAmount = finalAmount; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public void addItem(OrderItem item) { this.items.add(item); }

    @Override
    public String toString() {
        return "Order{id=%d, customerId=%d, total=%.2f, status='%s', items=%d}"
                .formatted(id, customerId, finalAmount, orderStatus, items.size());
    }
}
