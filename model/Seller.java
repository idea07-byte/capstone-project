package model;

public class Seller extends User {
    private final String storeName;

    public Seller(int id, String name, String email, String storeName) {
        super(id, name, email);
        this.storeName = storeName;
    }

    public String getStoreName() { return storeName; }
    @Override public String getRole() { return "Seller"; }
}
