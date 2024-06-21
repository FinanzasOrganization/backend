package com.wheelcredit.Backend.controller;

import com.wheelcredit.Backend.dto.ConsolidatedAccountResponse;
import com.wheelcredit.Backend.dto.TransactionRequestDto;
import com.wheelcredit.Backend.model.Customer;
import com.wheelcredit.Backend.model.Transaction;
import com.wheelcredit.Backend.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/go-finance/v1/transaction")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Transactional
    @PostMapping("/{customerId}")
    public ResponseEntity<List<Transaction>> creteTransaction(@RequestBody TransactionRequestDto transaction, @PathVariable(name = "customerId") Long customerId) {
        return ResponseEntity.ok(transactionService.save(transaction, customerId));
    }

    @Transactional(readOnly = true)
    @GetMapping("/consolidate/{customerId}")
    public ResponseEntity<ConsolidatedAccountResponse> consolidateTransaction(@PathVariable(name = "customerId") Long customerId) {
        ConsolidatedAccountResponse response = transactionService.consolidateAccount(customerId);
        return ResponseEntity.ok(response);
    }

    @Transactional
    @GetMapping("/{transactionId}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable(name = "transactionId") Long transactionId) {
        Transaction transaction = transactionService.getTransactionById(transactionId);
        return ResponseEntity.ok(transaction);
    }

    @Transactional
    @GetMapping("/{customerId}")
    public ResponseEntity<List<Transaction>> getAllTransactions(@PathVariable(name = "customerId") Long customerId) {
        List<Transaction> transactions = transactionService.findAll(customerId);
        return ResponseEntity.ok(transactions);
    }

    @Transactional
    @PutMapping("/{transactionId}")
    public ResponseEntity<Transaction> updateTransactionStatus(@PathVariable(name = "transactionId") Long transactionId, @RequestBody Transaction.TransactionStatus newStatus) {
        Transaction transaction = transactionService.updateStatus(transactionId, newStatus);
        return ResponseEntity.ok(transaction);
    }
}
