package com.wheelcredit.Backend.dto;

import com.wheelcredit.Backend.model.Client;
import com.wheelcredit.Backend.model.ConsolidatedData;
import com.wheelcredit.Backend.model.Customer;
import com.wheelcredit.Backend.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CustomerDto {

    private Long id;

    private String name;
    private String address;
    private String phone;
    private String email;
    private BigDecimal creditLimit;
    private LocalDate monthlyPaymentDate;
    private BigDecimal penaltyInterestRate;
    private AccountStatus accountStatus;
    private Client client;
    private List<Transaction> transactions;
    private ConsolidatedData consolidatedData;
    public enum AccountStatus {
        ACTIVE,
        INACTIVE
    }


}
