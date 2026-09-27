package com.craftora.craftora_backend.repository;

import com.craftora.craftora_backend.model.CustomerAddress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
    List<CustomerAddress> findAllByCustomer_IdOrderByDefaultAddressDescIdDesc(Long customerId);
    Optional<CustomerAddress> findByIdAndCustomer_Id(Long id, Long customerId);
}
