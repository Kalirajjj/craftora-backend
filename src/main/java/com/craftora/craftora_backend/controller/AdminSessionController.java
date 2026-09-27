package com.craftora.craftora_backend.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminSessionController {
    @GetMapping("/session")
    public ResponseEntity<Map<String, String>> verifyAdmin(Authentication authentication) {
        return ResponseEntity.ok(Map.of("username", authentication.getName()));
    }
}
