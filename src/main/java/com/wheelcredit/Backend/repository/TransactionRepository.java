package com.wheelcredit.Backend.repository;

import com.wheelcredit.Backend.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByCustomerId(long customerId);

    @Query("SELECT t FROM Transaction t WHERE t.customer.client.id = :clientId")
    List<Transaction> findByClientId(@Param("clientId") Long clientId);


}
