package model;

import java.sql.Timestamp;

public abstract class User {
    private final int id;
    private String name;
    private String email;
    private String phone;
    private String password;
    private final Role role;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    protected User(int id, String name, String email, String password, Role role) {
        this(id, name, email, null, password, role, "ACTIVE", null, null);
    }

    protected User(int id, String name, String email, String phone, String password, Role role) {
        this(id, name, email, phone, password, role, "ACTIVE", null, null);
    }

    protected User(int id, String name, String email, String phone, String password, Role role, String status, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
    public String getStatus() { return status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setPassword(String password) { this.password = password; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "%s{id=%d, name='%s', email='%s', role='%s', status='%s'}".formatted(role, id, name, email, role, status);
    }
}
