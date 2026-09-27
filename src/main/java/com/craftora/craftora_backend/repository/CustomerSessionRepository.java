package com.craftora.craftora_backend.repository;

import com.craftora.craftora_backend.model.CustomerSession;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerSessionRepository extends JpaRepository<CustomerSession, Long> {
    Optional<CustomerSession> findByTokenHashAndExpiresAtAfter(String tokenHash, LocalDateTime now);
    void deleteByTokenHash(String tokenHash);
    void deleteByCustomerId(Long customerId);
}
