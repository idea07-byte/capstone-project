package model;

public class Address {
    private int id;
    private int customerId;
    private String fullName;
    private String phone;
    private String addressLine;
    private String city;
    private String state;
    private String pincode;
    private boolean isDefault;

    public Address() {}

    public Address(int id, int customerId, String fullName, String phone, String addressLine,
                   String city, String state, String pincode, boolean isDefault) {
        this.id = id;
        this.customerId = customerId;
        this.fullName = fullName;
        this.phone = phone;
        this.addressLine = addressLine;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.isDefault = isDefault;
    }

    public int getId() { return id; }
    public int getCustomerId() { return customerId; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getAddressLine() { return addressLine; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPincode() { return pincode; }
    public boolean isDefault() { return isDefault; }

    public void setId(int id) { this.id = id; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
}
