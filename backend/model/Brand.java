package model;

import java.sql.Timestamp;

public class Brand {
    private int id;
    private String name;
    private String description;
    private String logo;
    private String status;
    private Timestamp createdAt;

    public Brand() {}

    public Brand(int id, String name, String description, String logo, String status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.logo = logo;
        this.status = status;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getLogo() { return logo; }
    public String getStatus() { return status; }
    public Timestamp getCreatedAt() { return createdAt; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setLogo(String logo) { this.logo = logo; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
