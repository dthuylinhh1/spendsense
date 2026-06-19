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
    private final ComparisonInsightService comparisonInsightService;
    private final AiCycleInsightService aiCycleInsightService;

    public TransactionCompareController(
            TransactionRepository transactionRepository,
            StatementImportRepository statementImportRepository,
            ComparisonInsightService comparisonInsightService,
            AiCycleInsightService aiCycleInsightService
    ) {
        this.transactionRepository = transactionRepository;
        this.statementImportRepository = statementImportRepository;
        this.comparisonInsightService = comparisonInsightService;
        this.aiCycleInsightService = aiCycleInsightService;
    }

    @GetMapping("/transactions/compare")
    public String compareTransactions(
            @RequestParam(required = false) Long cycleAId,
            @RequestParam(required = false) Long cycleBId,
            @RequestParam(required = false, defaultValue = "false") boolean generateAiInsight,
            Model model
    ) {
        var statementImports = statementImportRepository.findAllByOrderByUploadedAtDesc();

        System.out.println("COMPARE PAGE IMPORT COUNT = " + statementImports.size());

        model.addAttribute("statementImports", statementImports);
        model.addAttribute("selectedCycleAId", cycleAId);
        model.addAttribute("selectedCycleBId", cycleBId);
        model.addAttribute("aiInsightRequested", generateAiInsight);

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

            Long differenceCentsRaw = cycleBTotalCents - cycleATotalCents;

            BigDecimal cycleATotalDollars = centsToDollars(cycleATotalCents);
            BigDecimal cycleBTotalDollars = centsToDollars(cycleBTotalCents);
            BigDecimal differenceDollars = centsToDollars(differenceCentsRaw);
            String differenceDisplay = formatMoney(differenceDollars.abs());

            List<CategoryComparisonRow> categoryComparisonRows =
                    buildCategoryComparison(cycleAId, cycleBId);

            List<RecurringTransactionRow> recurringTransactionRows =
                    buildRecurringTransactions(cycleAId, cycleBId);

            Long recurringTotalCents = recurringTransactionRows.stream()
                    .mapToLong(RecurringTransactionRow::getAmountCents)
                    .sum();

            BigDecimal recurringTotalDollars = centsToDollars(recurringTotalCents);

            List<CycleOnlyTransactionRow> cycleAOnlyRows =
                    buildCycleOnlyTransactions(cycleAId, cycleBId);

            List<CycleOnlyTransactionRow> cycleBOnlyRows =
                    buildCycleOnlyTransactions(cycleBId, cycleAId);

            Long cycleAOnlyTotalCents = cycleAOnlyRows.stream()
                    .mapToLong(CycleOnlyTransactionRow::getAmountCents)
                    .sum();

            Long cycleBOnlyTotalCents = cycleBOnlyRows.stream()
                    .mapToLong(CycleOnlyTransactionRow::getAmountCents)
                    .sum();

            BigDecimal cycleAOnlyTotalDollars = centsToDollars(cycleAOnlyTotalCents);
            BigDecimal cycleBOnlyTotalDollars = centsToDollars(cycleBOnlyTotalCents);

            String higherCycleText =
                    comparisonInsightService.buildHigherCycleText(differenceDollars);

            String biggestCategoryChangeText =
                    comparisonInsightService.buildBiggestCategoryChangeText(categoryComparisonRows);

            model.addAttribute("hasComparison", true);

            model.addAttribute("cycleA", cycleA);
            model.addAttribute("cycleB", cycleB);

            model.addAttribute("cycleATotalDollars", formatMoney(cycleATotalDollars));
            model.addAttribute("cycleBTotalDollars", formatMoney(cycleBTotalDollars));
            model.addAttribute("differenceDollars", formatMoney(differenceDollars));
            model.addAttribute("differenceDisplay", differenceDisplay);
            model.addAttribute("differenceCentsRaw", differenceCentsRaw);

            model.addAttribute("categoryComparisonRows", categoryComparisonRows);

            model.addAttribute("recurringTransactionRows", recurringTransactionRows);
            model.addAttribute("recurringTotalDollars", formatMoney(recurringTotalDollars));

            model.addAttribute("cycleAOnlyRows", cycleAOnlyRows);
            model.addAttribute("cycleBOnlyRows", cycleBOnlyRows);
            model.addAttribute("cycleAOnlyTotalDollars", formatMoney(cycleAOnlyTotalDollars));
            model.addAttribute("cycleBOnlyTotalDollars", formatMoney(cycleBOnlyTotalDollars));

            model.addAttribute("higherCycleText", higherCycleText);
            model.addAttribute("biggestCategoryChangeText", biggestCategoryChangeText);

            if (generateAiInsight) {
                try {
                    String aiInsight = aiCycleInsightService.generateInsight(
                            cycleAId,
                            cycleBId,
                            cycleATotalDollars,
                            cycleBTotalDollars,
                            differenceDollars,
                            recurringTotalDollars,
                            cycleAOnlyTotalDollars,
                            cycleBOnlyTotalDollars,
                            categoryComparisonRows,
                            recurringTransactionRows,
                            cycleAOnlyRows,
                            cycleBOnlyRows
                    );
                    model.addAttribute("aiInsight", aiInsight);
                } catch (Exception e) {
                    model.addAttribute(
                            "aiInsightError",
                            "AI insight could not be generated right now: " + e.getMessage()
                    );
                }
            }
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

    private List<RecurringTransactionRow> buildRecurringTransactions(Long cycleAId, Long cycleBId) {
        List<TransactionEntity> cycleATransactions =
                transactionRepository.findByStatementImport_Id(cycleAId);

        List<TransactionEntity> cycleBTransactions =
                transactionRepository.findByStatementImport_Id(cycleBId);

        Set<String> cycleBKeys = new LinkedHashSet<>();

        for (TransactionEntity transaction : cycleBTransactions) {
            cycleBKeys.add(buildRecurringKey(transaction));
        }

        List<RecurringTransactionRow> rows = new ArrayList<>();
        Set<String> alreadyAdded = new LinkedHashSet<>();

        for (TransactionEntity transaction : cycleATransactions) {
            String key = buildRecurringKey(transaction);

            if (cycleBKeys.contains(key) && !alreadyAdded.contains(key)) {
                rows.add(new RecurringTransactionRow(
                        transaction.getDescription(),
                        transaction.getCardRef(),
                        transaction.getAmountCents(),
                        transaction.getBankCategory() == null
                                ? "Uncategorized"
                                : transaction.getBankCategory()
                ));

                alreadyAdded.add(key);
            }
        }

        rows.sort((a, b) -> Long.compare(
                Math.abs(b.getAmountCents()),
                Math.abs(a.getAmountCents())
        ));

        return rows;
    }

    private List<CycleOnlyTransactionRow> buildCycleOnlyTransactions(
            Long targetCycleId,
            Long comparisonCycleId
    ) {
        List<TransactionEntity> targetTransactions =
                transactionRepository.findByStatementImport_Id(targetCycleId);

        List<TransactionEntity> comparisonTransactions =
                transactionRepository.findByStatementImport_Id(comparisonCycleId);

        Set<String> comparisonKeys = new LinkedHashSet<>();

        for (TransactionEntity transaction : comparisonTransactions) {
            comparisonKeys.add(buildRecurringKey(transaction));
        }

        List<CycleOnlyTransactionRow> rows = new ArrayList<>();

        for (TransactionEntity transaction : targetTransactions) {
            String key = buildRecurringKey(transaction);

            if (!comparisonKeys.contains(key)) {
                rows.add(new CycleOnlyTransactionRow(
                        transaction.getPostedDate() == null
                                ? ""
                                : transaction.getPostedDate().toString(),
                        transaction.getDescription(),
                        transaction.getCardRef(),
                        transaction.getBankCategory() == null
                                ? "Uncategorized"
                                : transaction.getBankCategory(),
                        transaction.getAmountCents()
                ));
            }
        }

        rows.sort((a, b) -> Long.compare(
                Math.abs(b.getAmountCents()),
                Math.abs(a.getAmountCents())
        ));

        return rows;
    }

    private String buildRecurringKey(TransactionEntity transaction) {
        String description = normalizeText(transaction.getDescription());
        String cardRef = normalizeText(transaction.getCardRef());
        Long amountCents = transaction.getAmountCents();

        return description + "|" + cardRef + "|" + amountCents;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase();
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }

        return String.format("%,.2f", amount.setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal centsToDollars(Long cents) {
        if (cents == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(cents)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
