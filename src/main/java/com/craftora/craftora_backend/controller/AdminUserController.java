package com.craftora.craftora_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.craftora.craftora_backend.model.AdminUser;
import com.craftora.craftora_backend.repository.AdminUserRepository;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AdminUserController(AdminUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping
    public ResponseEntity<AdminUserResponse> createAdminUser(@RequestBody CreateAdminUserRequest request) {
        String username = request.username() == null ? "" : request.username().trim();
        String password = request.password() == null ? "" : request.password();

        if (!username.matches("[A-Za-z0-9._-]{3,80}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Username must be 3–80 characters using letters, numbers, dots, underscores, or hyphens.");
        }
        if (password.length() < 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at least 12 characters.");
        }
        if (users.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That username already exists.");
        }

        AdminUser saved = users.save(new AdminUser(username, passwordEncoder.encode(password)));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AdminUserResponse(saved.getId(), saved.getUsername(), saved.getRole()));
    }

    public record CreateAdminUserRequest(String username, String password) {}
    public record AdminUserResponse(Long id, String username, String role) {}
}
