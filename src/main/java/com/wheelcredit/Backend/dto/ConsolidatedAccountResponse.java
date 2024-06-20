package com.wheelcredit.Backend.dto;

import com.wheelcredit.Backend.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsolidatedAccountResponse {
    private Long customerId;
    private BigDecimal totalBalance;
    private BigDecimal totalInterest;
    private BigDecimal creditUsed;
    private List<Transaction> transactions;
}
