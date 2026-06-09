package com.example.demo.transaction;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Controller
public class TransactionPageController {

    private final TransactionRepository transactionRepository;

    public TransactionPageController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/transactions/view")
    public String viewTransactions(
            @RequestParam(required = false) String cardRef,
            @RequestParam(required = false) String postedMonth,
            Model model
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

        long totalAmountCents = transactions.stream()
                .mapToLong(TransactionEntity::getAmountCents)
                .sum();

        BigDecimal totalAmount = BigDecimal.valueOf(totalAmountCents, 2);

        model.addAttribute("transactions", transactions);
        model.addAttribute("selectedCardRef", cardRef == null ? "" : cardRef);
        model.addAttribute("selectedPostedMonth", postedMonth == null ? "" : postedMonth);
        model.addAttribute("rowCount", transactions.size());
        model.addAttribute("totalAmount", totalAmount);

        return "transactions";
    }
}