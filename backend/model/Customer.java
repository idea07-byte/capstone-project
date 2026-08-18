package model;

import java.sql.Timestamp;

public class Customer extends User {
    public Customer(int id, String name, String email, String password) {
        super(id, name, email, password, Role.CUSTOMER);
    }

    public Customer(int id, String name, String email, String phone, String password) {
        super(id, name, email, phone, password, Role.CUSTOMER);
    }

    public Customer(int id, String name, String email, String phone, String password, String status, Timestamp createdAt, Timestamp updatedAt) {
        super(id, name, email, phone, password, Role.CUSTOMER, status, createdAt, updatedAt);
    }
}
