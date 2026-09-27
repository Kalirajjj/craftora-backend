package com.craftora.craftora_backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "customer_addresses")
public class CustomerAddress {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "customer_id", nullable = false)
    private CustomerAccount customer;
    @Column(nullable = false, length = 60) private String label;
    @Column(name = "recipient_name", nullable = false, length = 120) private String recipientName;
    @Column(nullable = false, length = 40) private String phone;
    @Column(name = "address_line_1", nullable = false, length = 180) private String addressLine1;
    @Column(name = "address_line_2", length = 180) private String addressLine2;
    @Column(nullable = false, length = 80) private String city;
    @Column(nullable = false, length = 80) private String state;
    @Column(nullable = false, length = 12) private String pincode;
    @Column(name = "is_default", nullable = false) private boolean defaultAddress;

    protected CustomerAddress() {}
    public CustomerAddress(CustomerAccount customer, String label, String recipientName, String phone, String addressLine1,
            String addressLine2, String city, String state, String pincode, boolean defaultAddress) {
        this.customer=customer; this.label=label; this.recipientName=recipientName; this.phone=phone;
        this.addressLine1=addressLine1; this.addressLine2=addressLine2; this.city=city; this.state=state;
        this.pincode=pincode; this.defaultAddress=defaultAddress;
    }
    public Long getId(){return id;} public Long getCustomerId(){return customer.getId();}
    public String getLabel(){return label;} public String getRecipientName(){return recipientName;} public String getPhone(){return phone;}
    public String getAddressLine1(){return addressLine1;} public String getAddressLine2(){return addressLine2;}
    public String getCity(){return city;} public String getState(){return state;} public String getPincode(){return pincode;}
    public boolean isDefaultAddress(){return defaultAddress;} public void setDefaultAddress(boolean value){defaultAddress=value;}
}
