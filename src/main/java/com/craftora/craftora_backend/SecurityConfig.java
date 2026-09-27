package com.craftora.craftora_backend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.craftora.craftora_backend.service.CustomerSessionFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final CustomerSessionFilter customerSessionFilter;
    public SecurityConfig(CustomerSessionFilter customerSessionFilter) { this.customerSessionFilter = customerSessionFilter; }
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                    .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/api/customer/auth/register", "/api/customer/auth/login", "/api/customer/auth/verify",
                            "/api/customer/auth/resend-verification", "/api/customer/auth/forgot-password", "/api/customer/auth/reset-password").permitAll()
                    .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
                    .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**", "/uploads/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/admin/session").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/admin/users", "/api/products").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/custom-requests").hasRole("CUSTOMER")
                    .requestMatchers(HttpMethod.GET, "/api/admin/custom-requests", "/api/admin/custom-requests/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/admin/custom-requests/**").hasRole("ADMIN")
                    .requestMatchers("/api/**").denyAll()
                    .anyRequest().permitAll())
            .httpBasic(Customizer.withDefaults());
        http.addFilterBefore(customerSessionFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
