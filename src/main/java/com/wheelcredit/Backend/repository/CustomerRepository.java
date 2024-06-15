package com.wheelcredit.Backend.repository;

import com.wheelcredit.Backend.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Boolean existsByEmail(String email);
    Customer findById(long id);
    Customer findByEmail(String email);
    List<Customer> findByClientId(long clientId);
}
