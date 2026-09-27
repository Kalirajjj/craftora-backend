package com.craftora.craftora_backend.repository;

import com.craftora.craftora_backend.model.CustomerActionToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerActionTokenRepository extends JpaRepository<CustomerActionToken, Long> {
    Optional<CustomerActionToken> findByTokenHashAndPurposeAndExpiresAtAfter(String tokenHash, String purpose, LocalDateTime now);
    void deleteByCustomerIdAndPurpose(Long customerId, String purpose);
    void deleteByTokenHash(String tokenHash);
}
