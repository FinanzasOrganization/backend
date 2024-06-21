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

import static java.lang.Math.exp;
import static java.lang.Math.log;

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
        Transaction tr = new Transaction();
        if(trd.getTransactionType() == Transaction.TransactionType.PAYMENT){
            tr = transactionRepository.findById(trd.getPurchaseTransactionId())
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));
        }

        List<Transaction> transactions = new ArrayList<>();
        BigDecimal installmentAmount = BigDecimal.ZERO;
        BigDecimal interestAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalInterest = BigDecimal.ZERO;
        if(trd.getTransactionType() == Transaction.TransactionType.PURCHASE) {
            if (trd.getCreditType() == Transaction.CreditType.SINGLE_PAYMENT) {
                transactions.add(createSinglePaymentTransaction(customer, trd.getAmount(), trd.getDescription(), trd.getInterestRate(), trd.getInterestType(), trd.getTasaType(), trd.getCapitalizacionType(), transactionDate, paymentDate, customer.getMonthlyPaymentDate()));
                installmentAmount = calculateSinglePayment(trd.getAmount(), trd.getInterestRate(), trd.getInterestType(), trd.getTasaType(), trd.getCapitalizacionType(), customer.getMonthlyPaymentDate());
                interestAmount = calculateSingleInterestAmount(trd.getAmount(), trd.getInterestRate(), trd.getInterestType(), trd.getTasaType(), trd.getCapitalizacionType(), customer.getMonthlyPaymentDate());
                totalAmount = installmentAmount;
                totalInterest = interestAmount;
            } else if (trd.getCreditType() == Transaction.CreditType.MULTI_PAYMENT) {
                transactions.addAll(createMultiPaymentTransactions(customer, trd.getAmount(), trd.getDescription(), trd.getInterestRate(), transactionDate, trd.getInstallments(), trd.getInterestType(), trd.getTasaType()));
                installmentAmount = calculateMultiPayment(trd.getAmount(), trd.getInterestRate(), trd.getInstallments(), trd.getTasaType());
                interestAmount = installmentAmount.subtract(trd.getAmount().divide(BigDecimal.valueOf(trd.getInstallments()), MathContext.DECIMAL128));
                totalAmount = installmentAmount.multiply(BigDecimal.valueOf(trd.getInstallments()));
                totalInterest = interestAmount.multiply(BigDecimal.valueOf(trd.getInstallments()));
            }
        }
        else {
            transactions.add(createPaymentTransaction(customerId, trd.getPurchaseTransactionId(), tr.getAmount()));
        }

        if (trd.getTransactionType() == Transaction.TransactionType.PURCHASE)
        {
            validateCreditLimit(customerId, trd.getAmount());
        }
        transactionRepository.saveAll(transactions);

        if (trd.getTransactionType() == Transaction.TransactionType.PURCHASE)
        {
            ConsolidatedData consolidatedData = consolidatedDataRepository.findByCustomerId(customer.getId());

            if (consolidatedData == null) {
                consolidatedData = new ConsolidatedData();
                consolidatedData.setCustomer(customer);
                consolidatedData.setTotalAmount(totalAmount);
                consolidatedData.setTotalInterest(totalInterest);
                consolidatedData.setCreditUsed(trd.getAmount());
                consolidatedData.setTotalPaymentPending(totalAmount);
            } else {
                consolidatedData.setTotalAmount(consolidatedData.getTotalAmount().add(totalAmount));
                consolidatedData.setTotalInterest(consolidatedData.getTotalInterest().add(totalInterest));
                consolidatedData.setCreditUsed(consolidatedData.getCreditUsed().add(trd.getAmount()));
                consolidatedData.setTotalPaymentPending(consolidatedData.getTotalPaymentPending().add(totalAmount));
            }
            consolidatedDataRepository.save(consolidatedData);
        }

        return transactions;
    }

    private Transaction createSinglePaymentTransaction(Customer customer, BigDecimal amount, String description, BigDecimal interestRate, TransactionRequestDto.InterestType interestType, TransactionRequestDto.TasaType tasaType, TransactionRequestDto.CapitalizacionType capitalizacionType, LocalDate transactionDate, LocalDate paymentDate, LocalDate monthlyPaymentDate) {
        BigDecimal totalAmount = calculateSinglePayment(amount, interestRate, interestType, tasaType, capitalizacionType, monthlyPaymentDate);
        BigDecimal interestAmount = calculateSingleInterestAmount(amount, interestRate, interestType, tasaType, capitalizacionType, monthlyPaymentDate);
        Transaction.InterestType interestTypeTr = interestType == TransactionRequestDto.InterestType.NOMINAL ? Transaction.InterestType.NOMINAL : Transaction.InterestType.EFECTIVA;
        return Transaction.builder()
                .customer(customer)
                .penaltyInterestRate(customer.getPenaltyInterestRate())
                .amount(totalAmount)
                .interestAmount(interestAmount)
                .interestType(interestTypeTr)
                .description(description)
                .transactionDate(transactionDate)
                .dueDate(paymentDate)
                .transactionType(Transaction.TransactionType.PURCHASE)
                .status(Transaction.TransactionStatus.PENDING)
                .creditType(Transaction.CreditType.SINGLE_PAYMENT)
                .appliedInterest(interestRate)
                .build();
    }

    private Transaction createPaymentTransaction(Long customerId, Long purchaseTransactionId, BigDecimal paymentAmount)
    {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        Transaction purchaseTransaction = transactionRepository.findById(purchaseTransactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        LocalDate transactionDate = LocalDate.now();
        LocalDate paymentDate = customerService.getMaxPaymentDateForCurrentMonth(customerId);

        BigDecimal totalAmount = purchaseTransaction.getAmount();
        BigDecimal remainingAmount = totalAmount.subtract(paymentAmount);

        Transaction paymentTransaction = Transaction.builder()
                .customer(customer)
                .amount(paymentAmount)
                //.interestAmount(paymentAmount.multiply(purchaseTransaction.getAppliedInterest()))
                .description("Payment for transaction " + purchaseTransactionId)
                .transactionDate(transactionDate)
                .dueDate(paymentDate)
                .transactionType(Transaction.TransactionType.PAYMENT)
                .status(Transaction.TransactionStatus.PAID)
                .creditType(Transaction.CreditType.SINGLE_PAYMENT)
                //.appliedInterest(purchaseTransaction.getAppliedInterest())
                .build();

        if (remainingAmount.compareTo(BigDecimal.ZERO) == 0) {
            purchaseTransaction.setStatus(Transaction.TransactionStatus.PAID);
        }
        transactionRepository.save(purchaseTransaction);
        transactionRepository.save(paymentTransaction);

        ConsolidatedData consolidatedData = consolidatedDataRepository.findByCustomerId(customer.getId());
        consolidatedData.setTotalPaymentPending(consolidatedData.getTotalAmount().subtract(paymentAmount));
        consolidatedData.setCreditUsed(consolidatedData.getCreditUsed().subtract(paymentAmount));
        consolidatedData.setTotalPaymentAmount(consolidatedData.getTotalPaymentAmount().add(paymentAmount));
        consolidatedData.setTotalPenaltyPayment(consolidatedData.getTotalPenaltyPayment().add(purchaseTransaction.getPenaltyInterestAmount()));
        consolidatedData.setTotalPenaltyPending(consolidatedData.getTotalPenaltyPending().subtract(purchaseTransaction.getPenaltyInterestAmount()));
        consolidatedDataRepository.save(consolidatedData);

        return paymentTransaction;
    }

    private List<Transaction> createMultiPaymentTransactions(Customer customer, BigDecimal amount, String description, BigDecimal interestRate, LocalDate transactionDate, int installments, TransactionRequestDto.InterestType interestType, TransactionRequestDto.TasaType tasaType) {
        List<Transaction> transactions = new ArrayList<>();
        //BigDecimal installmentAmount = amount.divide(BigDecimal.valueOf(installments), BigDecimal.ROUND_HALF_UP);
        BigDecimal installmentAmount = calculateMultiPayment(amount, interestRate, installments, tasaType);
        BigDecimal interestAmount = installmentAmount.subtract(amount.divide(BigDecimal.valueOf(installments), MathContext.DECIMAL128));
        Transaction.InterestType interestTypeTr = interestType == TransactionRequestDto.InterestType.NOMINAL ? Transaction.InterestType.NOMINAL : Transaction.InterestType.EFECTIVA;


        for (int i = 0; i < installments; i++) {
            LocalDate paymentDate = customerService.getMaxPaymentDateForMonth(transactionDate.plusMonths(i + 1), customer.getId());
            transactions.add(Transaction.builder()
                    .customer(customer)
                    .penaltyInterestRate(customer.getPenaltyInterestRate())
                    .amount(installmentAmount)
                    .interestAmount(interestAmount)
                    .interestType(interestTypeTr)
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
    public Transaction getTransactionById(Long transactionId) {
        return transactionRepository.findById(transactionId).orElse(null);
    }

    @Override
    public List<Transaction> findAll(Long customerId) {
        verificatePenaltyCredit(customerId);
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

    public void verificatePenaltyCredit(Long customerId) {
        List<Transaction> transactions = transactionRepository.findAll();
        List<Transaction> transactionsByCustomer = transactionRepository.findByCustomerId(customerId);
        for (Transaction transaction : transactions) {
            if (transaction.getStatus() == Transaction.TransactionStatus.PENDING) {
                LocalDate currentDate = LocalDate.now();
                if (currentDate.isAfter(transaction.getDueDate())) {
                    int pastDays = (int) transaction.getDueDate().until(currentDate).getDays();
                    if (transaction.getInterestType() == Transaction.InterestType.EFECTIVA)
                    {
                        BigDecimal penaltyInterest = transaction.getPenaltyInterestRate().divide(BigDecimal.valueOf(100), MathContext.DECIMAL128);
                        BigDecimal base = BigDecimal.ONE.add(penaltyInterest);
                        BigDecimal exp = BigDecimal.valueOf(pastDays).divide(BigDecimal.valueOf(360), MathContext.DECIMAL128);
                        BigDecimal value = pow(base, exp).subtract(BigDecimal.ONE, MathContext.DECIMAL128);
                        BigDecimal penaltyAmount = transaction.getAmount().multiply(value, MathContext.DECIMAL128);
                        transaction.setPenaltyInterestAmount(penaltyAmount);
                    }
                    else {
                        BigDecimal penaltyInterest = transaction.getPenaltyInterestRate().divide(BigDecimal.valueOf(100), MathContext.DECIMAL128);
                        BigDecimal base = penaltyInterest.divide(BigDecimal.valueOf(360), MathContext.DECIMAL128).add(BigDecimal.ONE);
                        BigDecimal value = pow(base, BigDecimal.valueOf(pastDays)).subtract(BigDecimal.ONE, MathContext.DECIMAL128);
                        BigDecimal penaltyAmount = transaction.getAmount().multiply(value, MathContext.DECIMAL128);
                        transaction.setPenaltyInterestAmount(penaltyAmount);
                    }
                }
            }
        }
        ConsolidatedData consolidatedData = consolidatedDataRepository.findByCustomerId(customerId);
        BigDecimal totalPenaltyAmount = BigDecimal.ZERO;
        for (Transaction transaction : transactionsByCustomer) {
            totalPenaltyAmount = totalPenaltyAmount.add(transaction.getPenaltyInterestAmount());
        }
        consolidatedData.setTotalPenaltyAmount(totalPenaltyAmount);
        consolidatedData.setTotalPenaltyPending(consolidatedData.getTotalPenaltyPayment().add(totalPenaltyAmount));
    }

    public BigDecimal calculateSinglePayment(BigDecimal amount, BigDecimal interestRate, TransactionRequestDto.InterestType interestType, TransactionRequestDto.TasaType tasaType, TransactionRequestDto.CapitalizacionType capitalizacionType, LocalDate monthlyPaymentDate) {
        BigDecimal decimalInterest = interestRate.divide(BigDecimal.valueOf(100), MathContext.DECIMAL128);
        LocalDate transactionDate = LocalDate.now();
        int totalDays = (int) transactionDate.until(monthlyPaymentDate).getDays();

        BigDecimal m = getMValue(tasaType, capitalizacionType);
        BigDecimal n = BigDecimal.valueOf(totalDays);

        if (interestType == TransactionRequestDto.InterestType.NOMINAL) {
            // S = C * (1 + TN/m)^n
            BigDecimal base = BigDecimal.ONE.add(decimalInterest.divide(m, MathContext.DECIMAL128));
            BigDecimal result = amount.multiply(base.pow(n.intValue(), MathContext.DECIMAL128), MathContext.DECIMAL128);
            return result;
        } else {
            // S = C * (1 + TEA)^(n/m)
            BigDecimal exponent = n.divide(m, MathContext.DECIMAL128);
            BigDecimal base = BigDecimal.ONE.add(decimalInterest);
            BigDecimal result = amount.multiply(pow(base, exponent), MathContext.DECIMAL128);
            return result;
        }
    }

    public BigDecimal calculateSingleInterestAmount(BigDecimal amount, BigDecimal interestRate, TransactionRequestDto.InterestType interestType, TransactionRequestDto.TasaType tasaType, TransactionRequestDto.CapitalizacionType capitalizacionType, LocalDate monthlyPaymentDate) {
        BigDecimal decimalInterest = interestRate.divide(BigDecimal.valueOf(100), MathContext.DECIMAL128);
        if (interestType == TransactionRequestDto.InterestType.EFECTIVA) {
            LocalDate transactionDate = LocalDate.now();
            int totalDays = (int) transactionDate.until(monthlyPaymentDate).getDays();
            BigDecimal m = getMValue(tasaType, capitalizacionType);
            BigDecimal n = BigDecimal.valueOf(totalDays);

            // I = C * (1 + TEA)^(n/m) - C
            BigDecimal exponent = n.divide(m, MathContext.DECIMAL128);
            BigDecimal base = BigDecimal.ONE.add(decimalInterest);
            BigDecimal result = amount.multiply(pow(base, exponent), MathContext.DECIMAL128).subtract(amount);
            return result;
        } else {
            // I = C * (1 + TN/m)^n - C
            LocalDate transactionDate = LocalDate.now();
            int totalDays = (int) transactionDate.until(monthlyPaymentDate).getDays();
            BigDecimal m = getMValue(tasaType, capitalizacionType);
            BigDecimal n = BigDecimal.valueOf(totalDays);

            BigDecimal base = BigDecimal.ONE.add(decimalInterest.divide(m, MathContext.DECIMAL128));
            BigDecimal result = amount.multiply(base.pow(n.intValue(), MathContext.DECIMAL128), MathContext.DECIMAL128).subtract(amount);
            return result;
        }
    }

    public BigDecimal calculateMultiPayment(BigDecimal amount, BigDecimal interestRate, int installments, TransactionRequestDto.TasaType tasaType) {
        BigDecimal one = BigDecimal.ONE;
        BigDecimal decimalInterest = interestRate.divide(BigDecimal.valueOf(100), MathContext.DECIMAL128);
        BigDecimal TEM;

        // Convertir la tasa a mensual si es necesario
        if (tasaType != TransactionRequestDto.TasaType.MENSUAL) {
            int diasTEM = 30;
            int diasTEA = getDiasTEA(tasaType);
            BigDecimal exp = BigDecimal.valueOf(diasTEM).divide(BigDecimal.valueOf(diasTEA), MathContext.DECIMAL128);
            BigDecimal base = one.add(decimalInterest);
            TEM = pow(base, exp).subtract(one, MathContext.DECIMAL128);
        } else {
            TEM = decimalInterest.divide(BigDecimal.valueOf(100), MathContext.DECIMAL128);
        }

        BigDecimal n = BigDecimal.valueOf(installments);
        // (1 + TEM)
        BigDecimal firstBase = BigDecimal.ONE.add(TEM);
        //(1 + TEM)^n
        BigDecimal firstValue = pow(firstBase, n);
        ///(1 + TEM)^n - 1
        BigDecimal inferiorValue = firstValue.subtract(one, MathContext.DECIMAL128);
        //TEM * (1 + TEM)^n
        BigDecimal superiorValue = TEM.multiply(firstValue, MathContext.DECIMAL128);
        BigDecimal completeValue = superiorValue.divide(inferiorValue, MathContext.DECIMAL128);
        return amount.multiply(completeValue, MathContext.DECIMAL128);
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

    private BigDecimal getMValue(TransactionRequestDto.TasaType tasaType, TransactionRequestDto.CapitalizacionType capitalizacionType) {
        int tasaDays = 0;
        int capitalizacionDays = 0;

        switch (tasaType) {
            case MENSUAL:
                tasaDays = 30;
                break;
            case BIMESTRAL:
                tasaDays = 60;
                break;
            case TRIMESTRAL:
                tasaDays = 90;
                break;
            case SEMESTRAL:
                tasaDays = 180;
                break;
            case ANUAL:
                tasaDays = 360;
                break;
        }

        switch (capitalizacionType) {
            case DIARIA:
                capitalizacionDays = 1;
                break;
            case QUINCENAL:
                capitalizacionDays = 15;
                break;
            case MENSUAL:
                capitalizacionDays = 30;
                break;
        }

        return BigDecimal.valueOf(tasaDays).divide(BigDecimal.valueOf(capitalizacionDays), MathContext.DECIMAL128);
    }

    private int getDiasTEA(TransactionRequestDto.TasaType tasaType) {
        switch (tasaType) {
            case ANUAL:
                return 360;
            case SEMESTRAL:
                return 180;
            case TRIMESTRAL:
                return 90;
            case BIMESTRAL:
                return 60;
            default:
                throw new IllegalArgumentException("Tipo de tasa no soportado: " + tasaType);
        }
    }

    private BigDecimal pow(BigDecimal base, BigDecimal exponent) {
        int signOf2 = exponent.signum();
        double dn1 = base.doubleValue();
        exponent = exponent.multiply(new BigDecimal(signOf2)); // exponent is now positive
        BigDecimal remainderOf2 = exponent.remainder(BigDecimal.ONE);
        BigDecimal n2IntPart = exponent.subtract(remainderOf2);
        BigDecimal intPow = base.pow(n2IntPart.intValueExact(), MathContext.DECIMAL128);
        BigDecimal doublePow = BigDecimal.valueOf(Math.pow(dn1, remainderOf2.doubleValue()));

        BigDecimal result = intPow.multiply(doublePow, MathContext.DECIMAL128);
        if (signOf2 == -1) {
            result = BigDecimal.ONE.divide(result, MathContext.DECIMAL128);
        }
        return result;
    }


}
