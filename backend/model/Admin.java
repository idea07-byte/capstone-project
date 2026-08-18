package model;

import java.sql.Timestamp;

public class Admin extends User {
    public Admin(int id, String name, String email, String password) {
        super(id, name, email, password, Role.ADMIN);
    }

    public Admin(int id, String name, String email, String phone, String password, String status, Timestamp createdAt, Timestamp updatedAt) {
        super(id, name, email, phone, password, Role.ADMIN, status, createdAt, updatedAt);
    }
}
