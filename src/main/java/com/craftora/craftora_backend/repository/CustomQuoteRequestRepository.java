package com.craftora.craftora_backend.repository;

import com.craftora.craftora_backend.model.CustomQuoteRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomQuoteRequestRepository extends JpaRepository<CustomQuoteRequest, Long> {
    List<CustomQuoteRequest> findAllByDeletedFalseOrderByCreatedAtDesc();
    List<CustomQuoteRequest> findAllByDeletedTrueOrderByDeletedAtDesc();
    Optional<CustomQuoteRequest> findByReferenceCodeIgnoreCaseAndEmailIgnoreCaseAndDeletedFalse(String referenceCode, String email);
    List<CustomQuoteRequest> findAllByCustomerAccountIdAndDeletedFalseOrderByCreatedAtDesc(Long customerAccountId);
    List<CustomQuoteRequest> findAllByEmailIgnoreCaseAndCustomerAccountIsNull(String email);
}
