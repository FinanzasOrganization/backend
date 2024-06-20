package com.wheelcredit.Backend.service;

import com.wheelcredit.Backend.dto.ConsolidatedAccountResponse;
import com.wheelcredit.Backend.dto.TransactionRequestDto;
import com.wheelcredit.Backend.model.Transaction;

import java.util.List;

public interface TransactionService {
    List<Transaction> save(TransactionRequestDto transaction, Long accountId);
    Transaction findById(Long transactionId);
    List<Transaction> findAll();
    ConsolidatedAccountResponse consolidateAccount(Long customerId);
    Transaction updateStatus(Long transactionId, Transaction.TransactionStatus newStatus);
}
