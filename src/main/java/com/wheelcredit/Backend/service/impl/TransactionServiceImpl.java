package com.wheelcredit.Backend.service.impl;

import com.wheelcredit.Backend.dto.ConsolidatedAccountResponse;
import com.wheelcredit.Backend.dto.TransactionRequestDto;
import com.wheelcredit.Backend.model.ConsolidatedData;
import com.wheelcredit.Backend.model.Customer;
import com.wheelcredit.Backend.model.Transaction;
import com.wheelcredit.Backend.repository.ConsolidatedDataRepository;
import com.wheelcredit.Backend.repository.CustomerRepository;
import com.wheelcredit.Backend.repository.TransactionRepository;
import com.wheelcredit.Backend.service.CustomerService;
import com.wheelcredit.Backend.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ConsolidatedDataRepository consolidatedDataRepository;

    public List<Transaction> createTransaction(Long customerId, TransactionRequestDto trd) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        LocalDate transactionDate = LocalDate.now();
        LocalDate paymentDate = customerService.getMaxPaymentDateForCurrentMonth(customerId);

        List<Transaction> transactions = new ArrayList<>();
        BigDecimal installmentAmount = BigDecimal.ZERO;
        BigDecimal interestAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalInterest = BigDecimal.ZERO;
        if (trd.getCreditType() == Transaction.CreditType.SINGLE_PAYMENT) {
            transactions.add(createSinglePaymentTransaction(customer, trd.getAmount(), trd.getDescription(), trd.getInterestRate() , trd.getInterestType() , transactionDate, paymentDate));
            installmentAmount = calculateSinglePayment(trd.getAmount(), trd.getInterestRate(), trd.getInterestType());
            interestAmount = calculateSingleInterestAmount(trd.getAmount(), trd.getInterestRate(), trd.getInterestType());
            totalAmount = installmentAmount;
            totalInterest = interestAmount;
        } else if (trd.getCreditType() == Transaction.CreditType.MULTI_PAYMENT) {
            transactions.addAll(createMultiPaymentTransactions(customer, trd.getAmount(), trd.getDescription(), trd.getInterestRate() , transactionDate, trd.getInstallments(), trd.getInterestType()));
            installmentAmount = calculateMultiPayment(trd.getAmount(), trd.getInterestRate(), trd.getInstallments());
            interestAmount = installmentAmount.subtract(trd.getAmount().divide(BigDecimal.valueOf(trd.getInstallments()), MathContext.DECIMAL128));
            totalAmount = installmentAmount.multiply(BigDecimal.valueOf(trd.getInstallments()));
            totalInterest = interestAmount.multiply(BigDecimal.valueOf(trd.getInstallments()));
        }
        validateCreditLimit(customerId, trd.getAmount());
        transactionRepository.saveAll(transactions);

        ConsolidatedData consolidatedData = consolidatedDataRepository.findByCustomerId(customer.getId());

        if (consolidatedData == null) {
            consolidatedData = new ConsolidatedData();
            consolidatedData.setCustomer(customer);
            consolidatedData.setTotalAmount(totalAmount);
            consolidatedData.setTotalInterest(totalInterest);
            consolidatedData.setCreditUsed(trd.getAmount());
        } else {
            consolidatedData.setTotalAmount(consolidatedData.getTotalAmount().add(totalAmount));
            consolidatedData.setTotalInterest(consolidatedData.getTotalInterest().add(totalInterest));
            consolidatedData.setCreditUsed(consolidatedData.getCreditUsed().add(trd.getAmount()));
        }
        consolidatedDataRepository.save(consolidatedData);

        return transactions;
    }

    private Transaction createSinglePaymentTransaction(Customer customer, BigDecimal amount, String description, BigDecimal interestRate, TransactionRequestDto.InterestType interestType, LocalDate transactionDate, LocalDate paymentDate) {
        BigDecimal totalAmount = calculateSinglePayment(amount, interestRate, interestType);
        BigDecimal interestAmount = calculateSingleInterestAmount(amount, interestRate, interestType);
        return Transaction.builder()
                .customer(customer)
                .amount(totalAmount)
                .interestAmount(interestAmount)
                .description(description)
                .transactionDate(transactionDate)
                .dueDate(paymentDate)
                .transactionType(Transaction.TransactionType.PURCHASE)
                .status(Transaction.TransactionStatus.PENDING)
                .creditType(Transaction.CreditType.SINGLE_PAYMENT)
                .appliedInterest(interestRate)
                .build();
    }

    private List<Transaction> createMultiPaymentTransactions(Customer customer, BigDecimal amount, String description, BigDecimal interestRate, LocalDate transactionDate, int installments, TransactionRequestDto.InterestType interestType) {
        List<Transaction> transactions = new ArrayList<>();
        //BigDecimal installmentAmount = amount.divide(BigDecimal.valueOf(installments), BigDecimal.ROUND_HALF_UP);
        BigDecimal installmentAmount = calculateMultiPayment(amount, interestRate, installments);
        BigDecimal interestAmount = installmentAmount.subtract(amount.divide(BigDecimal.valueOf(installments), MathContext.DECIMAL128));


        for (int i = 0; i < installments; i++) {
            LocalDate paymentDate = customerService.getMaxPaymentDateForMonth(transactionDate.plusMonths(i), customer.getId());
            transactions.add(Transaction.builder()
                    .customer(customer)
                    .amount(installmentAmount)
                    .interestAmount(interestAmount)
                    .description(description)
                    .transactionDate(transactionDate)
                    .dueDate(paymentDate)
                    .transactionType(Transaction.TransactionType.PURCHASE)
                    .status(Transaction.TransactionStatus.PENDING)
                    .creditType(Transaction.CreditType.MULTI_PAYMENT)
                    .appliedInterest(interestRate)
                    .installments(installments)
                    .build());
        }

        return transactions;
    }

    @Override
    public List<Transaction> save(TransactionRequestDto dto, Long customerId) {
        return createTransaction(customerId, dto);
    }

    @Override
    public Transaction findById(Long transactionId) {
        return null;
    }

    @Override
    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    @Override
    public Transaction updateStatus(Long transactionId, Transaction.TransactionStatus newStatus) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        transaction.setStatus(newStatus);
        return transactionRepository.save(transaction);
    }

    @Override
    public ConsolidatedAccountResponse consolidateAccount(Long customerId) {
        List<Transaction> transactions = transactionRepository.findByCustomerId(customerId);
        ConsolidatedData consolidatedData = consolidatedDataRepository.findByCustomerId(customerId);

        ConsolidatedAccountResponse response = new ConsolidatedAccountResponse();
        response.setCustomerId(customerId);
        response.setTransactions(transactions);
        response.setTotalBalance(consolidatedData.getTotalAmount());
        response.setTotalInterest(consolidatedData.getTotalInterest());
        response.setCreditUsed(consolidatedData.getCreditUsed());
        return response;
    }

    public BigDecimal calculateSinglePayment(BigDecimal amount, BigDecimal interestRate, TransactionRequestDto.InterestType interestType) {
        BigDecimal decimalInterest = interestRate.divide(BigDecimal.valueOf(100), MathContext.DECIMAL128);
        if (interestType == interestType.EFECTIVA) {
            BigDecimal effectiveRate = (BigDecimal.ONE.add(decimalInterest)).pow(1).subtract(BigDecimal.ONE);
            return amount.add(amount.multiply(effectiveRate));
        } else {
            return amount.add(amount.multiply(decimalInterest));
        }
    }

    public BigDecimal calculateSingleInterestAmount(BigDecimal amount, BigDecimal interestRate, TransactionRequestDto.InterestType interestType) {
        BigDecimal decimalInterest = interestRate.divide(BigDecimal.valueOf(100), MathContext.DECIMAL128);
        if (interestType == TransactionRequestDto.InterestType.EFECTIVA) {
            BigDecimal effectiveRate = (BigDecimal.ONE.add(decimalInterest)).pow(1).subtract(BigDecimal.ONE);
            return amount.multiply(effectiveRate);
        } else {
            return amount.multiply(decimalInterest);
        }
    }

    public BigDecimal calculateMultiPayment(BigDecimal amount, BigDecimal interestRate, int installments) {
        // TEP es la tasa de interés efectiva por período
        BigDecimal one = BigDecimal.ONE;
        BigDecimal decimalInterest = interestRate.divide(BigDecimal.valueOf(100), MathContext.DECIMAL128);
        BigDecimal tep = decimalInterest;
        // Calcular (1 + TEP)^-n
        BigDecimal onePlusTep = one.add(tep);
        BigDecimal negativePower = onePlusTep.pow(-installments, MathContext.DECIMAL128);
        // Calcular denominador: 1 - (1 + TEP)^-n
        BigDecimal denominator = one.subtract(negativePower, MathContext.DECIMAL128);
        // Calcular numerador: C * TEP
        BigDecimal numerator = amount.multiply(tep, MathContext.DECIMAL128);
        // Calcular R: (C * TEP) / (1 - (1 + TEP)^-n)
        BigDecimal payment = numerator.divide(denominator, MathContext.DECIMAL128);
        return payment;
    }

    public BigDecimal calculateMultiPaymentWithGracePeriod(BigDecimal amount, BigDecimal annualInterestRate, int installments, TransactionRequestDto.InterestType interestType, int gracePeriod) {
        BigDecimal monthlyRate;
        if (interestType == interestType.EFECTIVA) {
            monthlyRate = (BigDecimal.ONE.add(annualInterestRate)).pow(1 / 12).subtract(BigDecimal.ONE);
        } else {
            monthlyRate = annualInterestRate.divide(BigDecimal.valueOf(12), MathContext.DECIMAL128); // Interés nominal mensual
        }

        BigDecimal adjustedAmount = amount.multiply(BigDecimal.ONE.add(monthlyRate).pow(gracePeriod, MathContext.DECIMAL128));

        BigDecimal numerator = adjustedAmount.multiply(monthlyRate);
        BigDecimal denominator = BigDecimal.ONE.subtract(
                BigDecimal.ONE.add(monthlyRate).pow(-installments, MathContext.DECIMAL128)
        );
        return numerator.divide(denominator, MathContext.DECIMAL128);
    }

    public void validateCreditLimit(Long customerId, BigDecimal amount) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        ConsolidatedData consolidatedData = consolidatedDataRepository.findByCustomerId(customerId);
        if (consolidatedData == null) {
            if (amount.compareTo(customer.getCreditLimit()) > 0) {
                throw new RuntimeException("Credit limit exceeded");
            }
        } else {
            if (amount.add(consolidatedData.getCreditUsed()).compareTo(customer.getCreditLimit()) > 0) {
                throw new RuntimeException("Credit limit exceeded");
            }
        }
    }



}
