package model;

import java.sql.Timestamp;

public class Vendor {
    private int id;
    private int userId;
    private String businessName;
    private String ownerName;
    private String description;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String approvalStatus;
    private Timestamp createdAt;
    private String ownerEmail;
    private String ownerPhone;
    private String ownerNameFromUser;

    public Vendor() {}

    public Vendor(int id, int userId, String businessName, String ownerName, String description,
                  String address, String city, String state, String pincode, String approvalStatus) {
        this.id = id;
        this.userId = userId;
        this.businessName = businessName;
        this.ownerName = ownerName;
        this.description = description;
        this.address = address;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.approvalStatus = approvalStatus;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getBusinessName() { return businessName; }
    public String getOwnerName() { return ownerName; }
    public String getDescription() { return description; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPincode() { return pincode; }
    public String getApprovalStatus() { return approvalStatus; }
    public Timestamp getCreatedAt() { return createdAt; }
    public String getOwnerEmail() { return ownerEmail; }
    public String getOwnerPhone() { return ownerPhone; }

    public void setId(int id) { this.id = id; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public void setDescription(String description) { this.description = description; }
    public void setAddress(String address) { this.address = address; }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
    public void setOwnerPhone(String ownerPhone) { this.ownerPhone = ownerPhone; }
}
