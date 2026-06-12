package com.example.demo.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CategoryComparisonRow {

    private String category;
    private Long cycleACents;
    private Long cycleBCents;

    public CategoryComparisonRow(String category, Long cycleACents, Long cycleBCents) {
        this.category = category;
        this.cycleACents = cycleACents == null ? 0L : cycleACents;
        this.cycleBCents = cycleBCents == null ? 0L : cycleBCents;
    }

    public String getCategory() {
        return category;
    }

    public Long getCycleACents() {
        return cycleACents;
    }

    public Long getCycleBCents() {
        return cycleBCents;
    }

    public Long getDifferenceCents() {
        return cycleBCents - cycleACents;
    }

    public BigDecimal getCycleADollars() {
        return centsToDollars(cycleACents);
    }

    public BigDecimal getCycleBDollars() {
        return centsToDollars(cycleBCents);
    }

    public BigDecimal getDifferenceDollars() {
        return centsToDollars(getDifferenceCents());
    }

    private BigDecimal centsToDollars(Long cents) {
        return BigDecimal.valueOf(cents)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}