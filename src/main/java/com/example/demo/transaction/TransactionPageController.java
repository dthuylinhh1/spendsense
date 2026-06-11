package com.example.demo.transaction;

import com.example.demo.repository.StatementImportRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Controller
public class TransactionPageController {

    private final TransactionRepository transactionRepository;
    private final StatementImportRepository statementImportRepository;

    public TransactionPageController(
        TransactionRepository transactionRepository,
        StatementImportRepository statementImportRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.statementImportRepository = statementImportRepository;
    }

    @GetMapping("/transactions/view")
    public String viewTransactions(
            @RequestParam(required = false) String cardRef,
            @RequestParam(required = false) String postedMonth,
            @RequestParam(required = false) Long statementImportId,
            Model model
    ) {
        List<TransactionEntity> transactions;

        boolean hasCardRef = cardRef != null && !cardRef.isBlank();
        boolean hasPostedMonth = postedMonth != null && !postedMonth.isBlank();
        boolean hasStatementImport = statementImportId != null;

        if (hasStatementImport) {
            if (hasCardRef) {
                transactions = transactionRepository
                        .findByStatementImport_IdAndCardRefOrderByPostedDateDescIdDesc(
                                statementImportId,
                                cardRef
                        );
            } else {
                transactions = transactionRepository
                        .findByStatementImport_IdOrderByPostedDateDescIdDesc(statementImportId);
            }
        } else if (hasPostedMonth) {
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
        model.addAttribute("selectedStatementImportId", statementImportId == null ? "" : statementImportId);
        model.addAttribute("rowCount", transactions.size());
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("statementImports", statementImportRepository.findAllByOrderByUploadedAtDesc());

       if (hasStatementImport) {
            var statementImport = statementImportRepository
                    .findById(statementImportId)
                    .orElseThrow();

            LocalDate cycleStart = statementImport.getStatementStartDate();
            LocalDate cycleEnd = statementImport.getStatementEndDate();

            Long totalSpendingCents = transactionRepository
                    .getTotalSpendingCentsByStatementImport(statementImportId);

            BigDecimal totalSpendingDollars = BigDecimal.valueOf(totalSpendingCents)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            long transactionCount = transactionRepository
                    .countTransactionsByStatementImport(statementImportId);

            List<SummaryRow> spendingByCard = transactionRepository
                    .getSpendingByCardForStatementImport(statementImportId)
                    .stream()
                    .map(row -> new SummaryRow(
                            row[0] == null ? "Unknown card" : row[0].toString(),
                            ((Number) row[1]).longValue()
                    ))
                    .toList();

            List<SummaryRow> topCategories = transactionRepository
                    .getTopCategoriesForStatementImport(statementImportId)
                    .stream()
                    .limit(3)
                    .map(row -> new SummaryRow(
                            row[0] == null ? "Uncategorized" : row[0].toString(),
                            ((Number) row[1]).longValue()
                    ))
                    .toList();

            model.addAttribute("cycleStart", cycleStart);
            model.addAttribute("cycleEnd", cycleEnd);
            model.addAttribute("transactionCount", transactionCount);
            model.addAttribute("totalSpendingDollars", totalSpendingDollars);
            model.addAttribute("spendingByCard", spendingByCard);
            model.addAttribute("topCategories", topCategories);
        }
        return "transactions";
    }
}