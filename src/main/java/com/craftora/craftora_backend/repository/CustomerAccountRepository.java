package com.craftora.craftora_backend.repository;

import com.craftora.craftora_backend.model.CustomerAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, Long> {
    Optional<CustomerAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
