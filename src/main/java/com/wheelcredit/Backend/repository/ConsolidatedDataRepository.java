package com.wheelcredit.Backend.repository;

import com.wheelcredit.Backend.model.ConsolidatedData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsolidatedDataRepository extends JpaRepository<ConsolidatedData, Long> {
    ConsolidatedData findByCustomerId(long customerId);
}
