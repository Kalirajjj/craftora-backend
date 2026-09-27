package com.craftora.craftora_backend.controller;

import com.craftora.craftora_backend.controller.CustomerAuthController.CustomerResponse;
import com.craftora.craftora_backend.model.*;
import com.craftora.craftora_backend.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/customer")
public class CustomerAccountController {
    private final CustomerAccountRepository accounts;
    private final CustomerCartItemRepository cart;
    private final ProductRepository products;
    private final CustomQuoteRequestRepository quotes;
    private final CustomerAddressRepository addresses;

    public CustomerAccountController(CustomerAccountRepository accounts, CustomerCartItemRepository cart,
            ProductRepository products, CustomQuoteRequestRepository quotes, CustomerAddressRepository addresses) {
        this.accounts = accounts; this.cart = cart; this.products = products; this.quotes = quotes; this.addresses = addresses;
    }

    @GetMapping("/profile")
    public CustomerResponse profile(Authentication auth) { return response(customer(auth)); }

    @PutMapping("/profile")
    @Transactional
    public CustomerResponse updateProfile(Authentication auth, @RequestBody ProfileUpdate update) {
        CustomerAccount account = customer(auth);
        if (update.fullName() == null || update.fullName().isBlank() || update.fullName().trim().length() > 120)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter your name (up to 120 characters).");
        if (update.phone() != null && update.phone().length() > 40) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is too long.");
        account.setFullName(update.fullName().trim()); account.setPhone(update.phone() == null ? null : update.phone().trim());
        return response(accounts.save(account));
    }

    @GetMapping("/cart")
    @Transactional(readOnly = true)
    public List<CartLineResponse> getCart(Authentication auth) {
        return cart.findAllByCustomerId(customer(auth).getId()).stream()
                .map(item -> new CartLineResponse(product(item.getProduct()), item.getQuantity(), item.getSelectedColor())).toList();
    }

    @PutMapping("/cart")
    @Transactional
    public List<CartLineResponse> saveCart(Authentication auth, @RequestBody List<CartLineRequest> updates) {
        CustomerAccount account = customer(auth);
        if (updates.size() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart has too many items.");
        cart.deleteAllByCustomerId(account.getId());
        for (CartLineRequest line : updates) {
            if (line.quantity() < 1 || line.quantity() > 99) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart quantity must be between 1 and 99.");
            Product product = products.findById(line.productId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "A cart product is no longer available."));
            String color = line.selectedColor() == null || line.selectedColor().isBlank() ? "Default" : line.selectedColor().trim();
            if (color.length() > 80) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected color is too long.");
            cart.save(new CustomerCartItem(account, product, line.quantity(), color));
        }
        return cart.findAllByCustomerId(account.getId()).stream()
                .map(item -> new CartLineResponse(product(item.getProduct()), item.getQuantity(), item.getSelectedColor())).toList();
    }

    @GetMapping("/quotes")
    @Transactional(readOnly = true)
    public List<CustomerQuoteResponse> getQuotes(Authentication auth) {
        return quotes.findAllByCustomerAccountIdAndDeletedFalseOrderByCreatedAtDesc(customer(auth).getId()).stream()
                .map(quote -> new CustomerQuoteResponse(quote.getReferenceCode(), quote.getProductType(), quote.getDescription(),
                        quote.getQuantity(), quote.getPreferredColor(), quote.getAdditionalNotes(), quote.getStatus(),
                        quote.getCreatedAt(), quote.getRequiredDate())).toList();
    }

    @GetMapping("/addresses")
    public List<AddressResponse> getAddresses(Authentication auth) {
        return addresses.findAllByCustomer_IdOrderByDefaultAddressDescIdDesc(customer(auth).getId()).stream()
                .map(this::addressResponse).toList();
    }

    @PostMapping("/addresses")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse addAddress(Authentication auth, @RequestBody AddressRequest input) {
        CustomerAccount account = customer(auth);
        if (addresses.findAllByCustomer_IdOrderByDefaultAddressDescIdDesc(account.getId()).size() >= 10)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can save up to 10 addresses.");
        validateAddress(input);
        List<CustomerAddress> current = addresses.findAllByCustomer_IdOrderByDefaultAddressDescIdDesc(account.getId());
        boolean makeDefault = input.defaultAddress() || current.isEmpty();
        if (makeDefault) current.forEach(address -> address.setDefaultAddress(false));
        CustomerAddress saved = addresses.save(new CustomerAddress(account, input.label().trim(), input.recipientName().trim(),
                input.phone().trim(), input.addressLine1().trim(), clean(input.addressLine2()), input.city().trim(),
                input.state().trim(), input.pincode().trim(), makeDefault));
        return addressResponse(saved);
    }

    @DeleteMapping("/addresses/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(Authentication auth, @PathVariable Long id) {
        Long customerId = customer(auth).getId();
        CustomerAddress address = addresses.findByIdAndCustomer_Id(id, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found."));
        boolean wasDefault = address.isDefaultAddress();
        addresses.delete(address);
        if (wasDefault) addresses.findAllByCustomer_IdOrderByDefaultAddressDescIdDesc(customerId).stream().findFirst()
                .ifPresent(next -> { next.setDefaultAddress(true); addresses.save(next); });
    }

    private CustomerAccount customer(Authentication auth) { return (CustomerAccount) auth.getPrincipal(); }
    private CustomerResponse response(CustomerAccount a) { return new CustomerResponse(a.getId(), a.getEmail(), a.getFullName(), a.getPhone(), a.isVerified()); }
    private CartProductResponse product(Product p) {
        List<CartProductImage> images = p.getImages().stream().map(i -> new CartProductImage(i.getId(), i.getImageUrl(), i.getDisplayOrder())).toList();
        return new CartProductResponse(p.getId(), p.getName(), p.getCategory(), p.getPrice(), p.getDescription(), p.getImageUrl(),
                p.getImageUrl(), images, p.getAvailableColors(), p.getRating(), p.getFeatured(), p.getDimensions(), p.getWeightGrams(), p.getInStock(), p.getSlug(), p.getBadge());
    }
    private AddressResponse addressResponse(CustomerAddress a) {
        return new AddressResponse(a.getId(), a.getLabel(), a.getRecipientName(), a.getPhone(), a.getAddressLine1(),
                a.getAddressLine2(), a.getCity(), a.getState(), a.getPincode(), a.isDefaultAddress());
    }
    private void validateAddress(AddressRequest a) {
        if (a.label()==null || a.label().isBlank() || a.label().length()>60 || a.recipientName()==null || a.recipientName().isBlank() || a.recipientName().length()>120
                || a.phone()==null || a.phone().isBlank() || a.phone().length()>40 || a.addressLine1()==null || a.addressLine1().isBlank() || a.addressLine1().length()>180
                || (a.addressLine2()!=null && a.addressLine2().length()>180) || a.city()==null || a.city().isBlank() || a.city().length()>80
                || a.state()==null || a.state().isBlank() || a.state().length()>80 || a.pincode()==null || !a.pincode().matches("[A-Za-z0-9 -]{3,12}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Complete the address fields with valid lengths.");
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    public record ProfileUpdate(String fullName, String phone) {}
    public record CartLineRequest(Long productId, int quantity, String selectedColor) {}
    public record CartLineResponse(CartProductResponse product, int quantity, String selectedColor) {}
    public record CartProductImage(Long id, String url, int displayOrder) {}
    public record CartProductResponse(Long id, String name, String category, java.math.BigDecimal price, String description,
            String image, String imageUrl, List<CartProductImage> images, List<String> availableColors, java.math.BigDecimal rating,
            Boolean featured, String dimensions, Integer weightGrams, Boolean inStock, String slug, String badge) {}
    public record CustomerQuoteResponse(String referenceCode, String productType, String description, int quantity,
            String preferredColor, String additionalNotes, QuoteStatus status, LocalDateTime createdAt, LocalDate requiredDate) {}
    public record AddressRequest(String label, String recipientName, String phone, String addressLine1, String addressLine2,
            String city, String state, String pincode, boolean defaultAddress) {}
    public record AddressResponse(Long id, String label, String recipientName, String phone, String addressLine1,
            String addressLine2, String city, String state, String pincode, boolean defaultAddress) {}
}
