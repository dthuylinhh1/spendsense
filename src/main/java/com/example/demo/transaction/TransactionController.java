package com.example.demo.transaction;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionRepository transactionRepository;

    public TransactionController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    public List<TransactionResponse> getTransactions(
            @RequestParam(required = false) String cardRef,
            @RequestParam(required = false) String postedMonth
    ) {
        List<TransactionEntity> transactions;

        boolean hasCardRef = cardRef != null && !cardRef.isBlank();
        boolean hasPostedMonth = postedMonth != null && !postedMonth.isBlank();

        if (hasPostedMonth) {
            YearMonth yearMonth = YearMonth.parse(postedMonth);
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();

            if (hasCardRef) {
                transactions = transactionRepository
                        .findByCardRefAndPostedDateBetweenOrderByPostedDateDescIdDesc(
                                cardRef, startDate, endDate
                        );
            } else {
                transactions = transactionRepository
                        .findByPostedDateBetweenOrderByPostedDateDescIdDesc(
                                startDate, endDate
                        );
            }
        } else if (hasCardRef) {
            transactions = transactionRepository.findByCardRefOrderByPostedDateDescIdDesc(cardRef);
        } else {
            transactions = transactionRepository.findAllByOrderByPostedDateDescIdDesc();
        }

        return transactions.stream()
                .map(tx -> new TransactionResponse(
                        tx.getId(),
                        tx.getTransDate(),
                        tx.getPostedDate(),
                        tx.getDescription(),
                        tx.getBankCategory(),
                        tx.getCardRef(),
                        tx.getAmountCents(),
                        tx.getCurrency(),
                        tx.getSource()
                ))
                .toList();
    }

    @DeleteMapping
    public void deleteAllTransactions() {
        transactionRepository.deleteAll();
    }
}