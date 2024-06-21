package com.wheelcredit.Backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "consolidate_data")
public class ConsolidatedData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_purchase_amount", precision = 17, scale = 7)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "total_interest", precision = 17, scale = 7)
    private BigDecimal totalInterest = BigDecimal.ZERO;

    @Column(name = "total_payment_amount", precision = 17, scale = 7)
    private BigDecimal totalPaymentAmount = BigDecimal.ZERO;

    @Column(name = "total_payment_pending", precision = 17, scale = 7)
    private BigDecimal totalPaymentPending = BigDecimal.ZERO;

    @Column(name = "total_penalty_amount", precision = 17, scale = 7)
    private BigDecimal totalPenaltyAmount = BigDecimal.ZERO;

    @Column(name = "total_penalty_payment", precision = 17, scale = 7)
    private BigDecimal totalPenaltyPayment = BigDecimal.ZERO;

    @Column(name = "total_penalty_pending", precision = 17, scale = 7)
    private BigDecimal totalPenaltyPending = BigDecimal.ZERO;

    @Column(name = "credit_used", precision = 17, scale = 7)
    private BigDecimal creditUsed = BigDecimal.ZERO;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
