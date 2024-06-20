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
public class TransactionRequestDto {
    private BigDecimal amount;
    private String description;
    private Transaction.CreditType creditType;
    private Transaction.TransactionType transactionType;
    private BigDecimal interestRate;
    private Integer installments;
    private InterestType interestType;
    private Integer tasaCapitalizada;

    public enum InterestType {
        NOMINAL,
        EFECTIVA
    }
}
