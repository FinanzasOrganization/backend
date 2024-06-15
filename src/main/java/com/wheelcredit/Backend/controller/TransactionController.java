package com.wheelcredit.Backend.controller;

import com.wheelcredit.Backend.dto.ConsolidatedAccountResponse;
import com.wheelcredit.Backend.dto.TransactionRequestDto;
import com.wheelcredit.Backend.model.Transaction;
import com.wheelcredit.Backend.service.TransactionService;
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
}
