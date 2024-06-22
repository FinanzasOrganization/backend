package com.wheelcredit.Backend.dto;


import com.wheelcredit.Backend.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentTransactionDto {
    private BigDecimal amount;
    private String description;
    private Transaction.TransactionType transactionType;
    private Long purchaseTransactionId;
}
