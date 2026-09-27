package com.craftora.craftora_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_action_tokens")
public class CustomerActionToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerAccount customer;
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(nullable = false, length = 20)
    private String purpose;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    protected CustomerActionToken() {}
    public CustomerActionToken(CustomerAccount customer, String tokenHash, String purpose, LocalDateTime expiresAt) {
        this.customer = customer; this.tokenHash = tokenHash; this.purpose = purpose; this.expiresAt = expiresAt;
    }
    public Long getId() { return id; }
    public CustomerAccount getCustomer() { return customer; }
    public String getPurpose() { return purpose; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
