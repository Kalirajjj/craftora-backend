package com.craftora.craftora_backend.service;

import com.craftora.craftora_backend.repository.CustomerSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CustomerSessionFilter extends OncePerRequestFilter {
    private final CustomerSessionRepository sessions;
    public CustomerSessionFilter(CustomerSessionRepository sessions) { this.sessions = sessions; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(7).trim();
            if (!token.isEmpty()) {
                try {
                    String tokenHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
                    sessions.findByTokenHashAndExpiresAtAfter(tokenHash, LocalDateTime.now()).ifPresent(session -> {
                        var auth = new UsernamePasswordAuthenticationToken(session.getCustomer(), null,
                                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
                } catch (Exception ignored) { SecurityContextHolder.clearContext(); }
            }
        }
        chain.doFilter(request, response);
    }
}
