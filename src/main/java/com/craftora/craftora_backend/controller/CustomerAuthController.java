package com.craftora.craftora_backend.controller;

import com.craftora.craftora_backend.model.CustomerAccount;
import com.craftora.craftora_backend.service.CustomerAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/auth")
public class CustomerAuthController {
    private final CustomerAccountService accounts;
    public CustomerAuthController(CustomerAccountService accounts) { this.accounts = accounts; }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Message register(@RequestBody RegisterRequest request) {
        accounts.register(request.email(), request.password(), request.fullName(), request.phone());
        return new Message("Check your email and follow the verification link to activate your account.");
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        var result = accounts.login(request.email(), request.password());
        return new LoginResponse(result.token(), customer(result.customer()));
    }

    @PostMapping("/verify")
    public Message verify(@RequestBody TokenRequest request) {
        accounts.verifyEmail(request.token());
        return new Message("Your email is verified. You can now sign in.");
    }

    @PostMapping("/resend-verification")
    public Message resendVerification(@RequestBody EmailRequest request) {
        accounts.resendVerification(request.email());
        return new Message("If an unverified account exists for that email, a verification link has been sent.");
    }

    @PostMapping("/forgot-password")
    public Message forgotPassword(@RequestBody EmailRequest request) {
        accounts.startPasswordReset(request.email());
        return new Message("If an account exists for that email, password reset instructions have been sent.");
    }

    @PostMapping("/reset-password")
    public Message resetPassword(@RequestBody ResetPasswordRequest request) {
        accounts.resetPassword(request.token(), request.password());
        return new Message("Password changed. Please sign in again.");
    }

    @PostMapping("/logout")
    public Message logout(Authentication authentication, @RequestHeader(value = "Authorization", required = false) String authorization) {
        accounts.logout(authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null);
        return new Message("Signed out.");
    }

    @GetMapping("/me")
    public CustomerResponse me(Authentication authentication) { return customer((CustomerAccount) authentication.getPrincipal()); }

    private static CustomerResponse customer(CustomerAccount account) {
        return new CustomerResponse(account.getId(), account.getEmail(), account.getFullName(), account.getPhone(), account.isVerified());
    }

    public record RegisterRequest(String email, String password, String fullName, String phone) {}
    public record LoginRequest(String email, String password) {}
    public record EmailRequest(String email) {}
    public record TokenRequest(String token) {}
    public record ResetPasswordRequest(String token, String password) {}
    public record CustomerResponse(Long id, String email, String fullName, String phone, boolean verified) {}
    public record LoginResponse(String token, CustomerResponse customer) {}
    public record Message(String message) {}
}
