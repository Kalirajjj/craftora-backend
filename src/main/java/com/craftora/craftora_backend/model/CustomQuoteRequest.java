package com.craftora.craftora_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "custom_quote_requests")
public class CustomQuoteRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_code", nullable = false, unique = true, length = 20)
    private String referenceCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 40)
    private String phone;

    @Column(name = "product_type", nullable = false, length = 120)
    private String productType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "preferred_color", length = 120)
    private String preferredColor;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "required_date")
    private LocalDate requiredDate;

    @Column(name = "additional_notes", columnDefinition = "TEXT")
    private String additionalNotes;

    @Column(name = "attachment_name", length = 255)
    private String attachmentName;

    @Column(name = "attachment_storage_name", length = 100)
    private String attachmentStorageName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuoteStatus status = QuoteStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_account_id")
    private CustomerAccount customerAccount;

    @PrePersist
    void beforeInsert() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (referenceCode == null) referenceCode = "RFQ-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        if (status == null) status = QuoteStatus.PENDING;
    }

    public Long getId() { return id; }
    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPreferredColor() { return preferredColor; }
    public void setPreferredColor(String preferredColor) { this.preferredColor = preferredColor; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public LocalDate getRequiredDate() { return requiredDate; }
    public void setRequiredDate(LocalDate requiredDate) { this.requiredDate = requiredDate; }
    public String getAdditionalNotes() { return additionalNotes; }
    public void setAdditionalNotes(String additionalNotes) { this.additionalNotes = additionalNotes; }
    public String getAttachmentName() { return attachmentName; }
    public void setAttachmentName(String attachmentName) { this.attachmentName = attachmentName; }
    public String getAttachmentStorageName() { return attachmentStorageName; }
    public void setAttachmentStorageName(String attachmentStorageName) { this.attachmentStorageName = attachmentStorageName; }
    public QuoteStatus getStatus() { return status; }
    public void setStatus(QuoteStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public CustomerAccount getCustomerAccount() { return customerAccount; }
    public void setCustomerAccount(CustomerAccount customerAccount) { this.customerAccount = customerAccount; }
}
