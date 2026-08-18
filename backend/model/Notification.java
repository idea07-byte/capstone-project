package model;

import java.sql.Timestamp;

public class Notification {
    private int id;
    private int userId;
    private String title;
    private String message;
    private String type;
    private boolean isRead;
    private Timestamp createdAt;

    public Notification() {}

    public Notification(int id, int userId, String title, String message, String type) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = false;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public boolean isRead() { return isRead; }
    public Timestamp getCreatedAt() { return createdAt; }

    public void setId(int id) { this.id = id; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
    public void setMessage(String message) { this.message = message; }
    public void setType(String type) { this.type = type; }
    public void setRead(boolean read) { isRead = read; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
