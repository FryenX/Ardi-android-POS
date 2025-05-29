package com.example.ardimart;

import java.io.Serializable;

public class User implements Serializable {
    private int id;
    private String name;
    private String username;

    private String level;
    public User(int id, String name, String username,String level) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.level = level;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getUserName() { return username; }
    public String getLevel() { return level; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setUserName(String username) { this.username = username; }
    public void setLevel(String level) { this.level = level; }
}