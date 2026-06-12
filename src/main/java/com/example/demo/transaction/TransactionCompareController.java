package com.example.demo.transaction;

import com.example.demo.entity.StatementImportEntity;
import com.example.demo.repository.StatementImportRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

@Controller
public class TransactionCompareController {

    private final TransactionRepository transactionRepository;
    private final StatementImportRepository statementImportRepository;

    public TransactionCompareController(
            TransactionRepository transactionRepository,
            StatementImportRepository statementImportRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.statementImportRepository = statementImportRepository;
    }

    @GetMapping("/transactions/compare")
    public String compareTransactions(
            @RequestParam(required = false) Long cycleAId,
            @RequestParam(required = false) Long cycleBId,
            Model model
    ) {
        var statementImports = statementImportRepository.findAllByOrderByUploadedAtDesc();

        System.out.println("COMPARE PAGE IMPORT COUNT = " + statementImports.size());

        model.addAttribute("statementImports", statementImports);
        model.addAttribute("selectedCycleAId", cycleAId);
        model.addAttribute("selectedCycleBId", cycleBId);

        boolean hasBothCycles = cycleAId != null && cycleBId != null;

        if (hasBothCycles) {
            StatementImportEntity cycleA = statementImportRepository
                    .findById(cycleAId)
                    .orElseThrow();

            StatementImportEntity cycleB = statementImportRepository
                    .findById(cycleBId)
                    .orElseThrow();

            Long cycleATotalCents = transactionRepository
                    .getTotalSpendingCentsByStatementImport(cycleAId);

            Long cycleBTotalCents = transactionRepository
                    .getTotalSpendingCentsByStatementImport(cycleBId);

            BigDecimal cycleATotalDollars = centsToDollars(cycleATotalCents);
            BigDecimal cycleBTotalDollars = centsToDollars(cycleBTotalCents);
            BigDecimal differenceDollars = centsToDollars(cycleBTotalCents - cycleATotalCents);

            List<CategoryComparisonRow> categoryComparisonRows =
                    buildCategoryComparison(cycleAId, cycleBId);

            model.addAttribute("hasComparison", true);
            model.addAttribute("cycleA", cycleA);
            model.addAttribute("cycleB", cycleB);
            model.addAttribute("cycleATotalDollars", cycleATotalDollars);
            model.addAttribute("cycleBTotalDollars", cycleBTotalDollars);
            model.addAttribute("differenceDollars", differenceDollars);
            model.addAttribute("categoryComparisonRows", categoryComparisonRows);
        } else {
            model.addAttribute("hasComparison", false);
        }

        return "transaction-compare";
    }

    private List<CategoryComparisonRow> buildCategoryComparison(Long cycleAId, Long cycleBId) {
        Map<String, Long> cycleAMap = toCategoryMap(
                transactionRepository.getCategoryTotalsByStatementImport(cycleAId)
        );

        Map<String, Long> cycleBMap = toCategoryMap(
                transactionRepository.getCategoryTotalsByStatementImport(cycleBId)
        );

        Set<String> allCategories = new LinkedHashSet<>();
        allCategories.addAll(cycleAMap.keySet());
        allCategories.addAll(cycleBMap.keySet());

        List<CategoryComparisonRow> rows = new ArrayList<>();

        for (String category : allCategories) {
            rows.add(new CategoryComparisonRow(
                    category,
                    cycleAMap.getOrDefault(category, 0L),
                    cycleBMap.getOrDefault(category, 0L)
            ));
        }

        rows.sort((a, b) -> Long.compare(
                Math.abs(b.getDifferenceCents()),
                Math.abs(a.getDifferenceCents())
        ));

        return rows;
    }

    private Map<String, Long> toCategoryMap(List<Object[]> rows) {
        Map<String, Long> map = new HashMap<>();

        for (Object[] row : rows) {
            String category = row[0] == null ? "Uncategorized" : row[0].toString();
            Long totalCents = ((Number) row[1]).longValue();
            map.put(category, totalCents);
        }

        return map;
    }

    private BigDecimal centsToDollars(Long cents) {
        return BigDecimal.valueOf(cents)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}