package model;

public class Customer extends User {
    public Customer(int id, String name, String email, String password) {
        super(id, name, email, password);
    }

    @Override public String getRole() { return "Customer"; }
}
