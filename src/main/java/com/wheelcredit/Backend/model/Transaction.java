package com.wheelcredit.Backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "amount", precision = 17, scale = 7)
    private BigDecimal amount;

    @Column(name = "interest_amount", precision = 17, scale = 7)
    private BigDecimal interestAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private TransactionType transactionType;

    @Column(name = "description", nullable = true)
    private String description;

    @Column(name = "applied_interest")
    private BigDecimal appliedInterest;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TransactionStatus status;

    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "credit_type")
    private CreditType creditType;

    // For MULTI_PAYMENT: Number of installments
    @Column(name = "installments")
    private Integer installments;

    public String getCustomerName() {
        return customer.getName();
    }

    public BigDecimal getCreditLimit() {
        return customer.getCreditLimit();
    }

    public enum TransactionType {
        PURCHASE,
        PAYMENT
    }

    public enum TransactionStatus {
        PENDING,
        PAID
    }

    public enum CreditType {
        SINGLE_PAYMENT, // For purchases paid on the following payment date
        MULTI_PAYMENT   // For purchases paid over several months
    }

}
