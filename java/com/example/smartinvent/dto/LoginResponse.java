package com.example.smartinvent.dto;

// This is what we send BACK to the browser after a successful login.
public class LoginResponse {

    private String token;
    private String username;
    private String role;
    private String name;

    public LoginResponse(String token, String username, String role, String name) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
