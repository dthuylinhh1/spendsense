package com.example.demo.transaction;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ComparisonInsightService {

    public String buildHigherCycleText(BigDecimal differenceDollars) {
        if (differenceDollars.compareTo(BigDecimal.ZERO) > 0) {
            return "Cycle B is higher than Cycle A by $" + formatMoney(differenceDollars.abs()) + ".";
        }

        if (differenceDollars.compareTo(BigDecimal.ZERO) < 0) {
            return "Cycle A is higher than Cycle B by $" + formatMoney(differenceDollars.abs()) + ".";
        }

        return "Cycle A and Cycle B have the same total spending.";
    }

    public String buildBiggestCategoryChangeText(List<CategoryComparisonRow> categoryComparisonRows) {
        if (categoryComparisonRows == null || categoryComparisonRows.isEmpty()) {
            return "No category change found.";
        }

        CategoryComparisonRow biggestChange = categoryComparisonRows.get(0);

        if (biggestChange.getDifferenceCents() > 0) {
            return "Biggest category change: "
                    + biggestChange.getCategory()
                    + " increased by $"
                    + formatMoney(biggestChange.getDifferenceDollars().abs())
                    + ".";
        }

        if (biggestChange.getDifferenceCents() < 0) {
            return "Biggest category change: "
                    + biggestChange.getCategory()
                    + " decreased by $"
                    + formatMoney(biggestChange.getDifferenceDollars().abs())
                    + ".";
        }

        return "Biggest category change: "
                + biggestChange.getCategory()
                + " had no change.";
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }

        return String.format("%,.2f", amount.setScale(2, RoundingMode.HALF_UP));
    }
}