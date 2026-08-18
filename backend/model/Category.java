package model;

import java.sql.Timestamp;

public class Category {
    private int id;
    private String name;
    private String description;
    private String image;
    private String status;
    private Timestamp createdAt;

    public Category() {}

    public Category(int id, String name, String description, String image, String status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.image = image;
        this.status = status;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImage() { return image; }
    public String getStatus() { return status; }
    public Timestamp getCreatedAt() { return createdAt; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setImage(String image) { this.image = image; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
