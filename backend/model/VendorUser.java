package model;

import java.sql.Timestamp;

public class VendorUser extends User {
    public VendorUser(int id, String name, String email, String password) {
        super(id, name, email, password, Role.VENDOR);
    }

    public VendorUser(int id, String name, String email, String phone, String password) {
        super(id, name, email, phone, password, Role.VENDOR);
    }

    public VendorUser(int id, String name, String email, String phone, String password, String status, Timestamp createdAt, Timestamp updatedAt) {
        super(id, name, email, phone, password, Role.VENDOR, status, createdAt, updatedAt);
    }
}
