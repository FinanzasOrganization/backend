package com.wheelcredit.Backend.service;

import com.wheelcredit.Backend.dto.ConsolidatedAccountResponse;
import com.wheelcredit.Backend.dto.TransactionRequestDto;
import com.wheelcredit.Backend.model.Transaction;

import java.util.List;

public interface TransactionService {
    List<Transaction> save(TransactionRequestDto transaction, Long accountId);
    Transaction getTransactionById(Long transactionId);
    List<Transaction> findAll(Long customerId);
    ConsolidatedAccountResponse consolidateAccount(Long customerId);

    List<Transaction> findByClientId(Long clientId);

    Transaction updateStatus(Long transactionId, Transaction.TransactionStatus newStatus);
}
