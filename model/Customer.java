package model;

public class Customer extends User {
    private final String shippingAddress;

    public Customer(int id, String name, String email, String shippingAddress) {
        super(id, name, email);
        this.shippingAddress = shippingAddress;
    }

    public String getShippingAddress() { return shippingAddress; }
    @Override public String getRole() { return "Customer"; }
}
