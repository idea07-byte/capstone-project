package model;

public class Customer extends User {
    private String shippingAddress;

    public Customer(int id, String name, String email, String password, String shippingAddress) {
        super(id, name, email, password);
        this.shippingAddress = shippingAddress;
    }

    public Customer(int id, String name, String email, String shippingAddress) {
        this(id, name, email, "", shippingAddress);
    }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    @Override public String getRole() { return "Customer"; }
}
