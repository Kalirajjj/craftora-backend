package com.craftora.craftora_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_sessions")
public class CustomerSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerAccount customer;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    protected CustomerSession() {}
    public CustomerSession(CustomerAccount customer, String tokenHash, LocalDateTime expiresAt) {
        this.customer = customer; this.tokenHash = tokenHash; this.expiresAt = expiresAt;
    }
    public Long getId() { return id; }
    public CustomerAccount getCustomer() { return customer; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
