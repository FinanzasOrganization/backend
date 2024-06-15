package com.wheelcredit.Backend.repository;

import com.wheelcredit.Backend.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByCustomerId(long customerId);
}
