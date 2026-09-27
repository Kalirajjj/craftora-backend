package com.craftora.craftora_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_accounts")
public class CustomerAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 254)
    private String email;
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;
    @Column(length = 40)
    private String phone;
    @Column(nullable = false)
    private boolean verified = false;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected CustomerAccount() {}
    public CustomerAccount(String email, String passwordHash, String fullName, String phone) {
        this.email = email; this.passwordHash = passwordHash; this.fullName = fullName; this.phone = phone;
    }
    @PrePersist void beforeInsert() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public void changePasswordHash(String value) { passwordHash = value; }
    public String getFullName() { return fullName; }
    public void setFullName(String value) { fullName = value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean value) { verified = value; }
}
