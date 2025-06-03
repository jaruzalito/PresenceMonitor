package com.example.pracaobecnosc;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String username;
    private String password;
    private String type;
    private String status;

    // Default constructor required by Room
    public User() {
    }

    // Constructor without ID (for registration)
    public User(String username, String password, String type) {
        this.username = username;
        this.password = password;
        this.type = type;
        this.status = "offline"; // Default status
    }

    // Full constructor
    public User(int id, String username, String password, String type, String status) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.type = type;
        this.status = status;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}