package com.craftora.craftora_backend.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.craftora.craftora_backend.model.AdminUser;
import com.craftora.craftora_backend.repository.AdminUserRepository;

@Service
public class AdminUserDetailsService implements UserDetailsService {
    private final AdminUserRepository users;

    public AdminUserDetailsService(AdminUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser admin = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Admin account not found."));
        return User.withUsername(admin.getUsername())
                .password(admin.getPasswordHash())
                .roles(admin.getRole())
                .disabled(!admin.isEnabled())
                .build();
    }
}
