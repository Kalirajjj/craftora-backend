package com.craftora.craftora_backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "customer_cart_items", uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "product_id", "selected_color"}))
public class CustomerCartItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "customer_id", nullable = false)
    private CustomerAccount customer;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Column(nullable = false)
    private int quantity;
    @Column(name = "selected_color", nullable = false, length = 80)
    private String selectedColor;

    protected CustomerCartItem() {}
    public CustomerCartItem(CustomerAccount customer, Product product, int quantity, String selectedColor) {
        this.customer = customer; this.product = product; this.quantity = quantity; this.selectedColor = selectedColor;
    }
    public Long getId() { return id; }
    public CustomerAccount getCustomer() { return customer; }
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public String getSelectedColor() { return selectedColor; }
}
