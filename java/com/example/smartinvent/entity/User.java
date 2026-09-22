package com.example.smartinvent.entity;

import jakarta.persistence.*;

// @Entity tells Hibernate: "this Java class maps to a database table"
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String phone;

    @Column(unique = true)
    private String username;

    // This will store the BCrypt-hashed password, never the plain text password
    private String password;

    // ROLE_ADMIN or ROLE_WORKER
    private String role;

    // ACTIVE or INACTIVE (used to "deactivate" a worker instead of deleting them)
    private String status = "ACTIVE";

    public User() {
    }

    // Getters and setters (kept plain and explicit on purpose - no Lombok -
    // so every field access is visible and easy to explain)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
