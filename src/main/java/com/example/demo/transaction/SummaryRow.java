package com.example.demo.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SummaryRow {

    private String label;
    private Long totalCents;

    public SummaryRow(String label, Long totalCents) {
        this.label = label;
        this.totalCents = totalCents;
    }

    public String getLabel() {
        return label;
    }

    public Long getTotalCents() {
        return totalCents;
    }

    public BigDecimal getTotalDollars() {
        return BigDecimal.valueOf(totalCents)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}