package com.example.demo.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RecurringTransactionRow {

    private final String description;
    private final String cardRef;
    private final Long amountCents;
    private final String category;

    public RecurringTransactionRow(
            String description,
            String cardRef,
            Long amountCents,
            String category
    ) {
        this.description = description;
        this.cardRef = cardRef;
        this.amountCents = amountCents;
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public String getCardRef() {
        return cardRef;
    }

    public Long getAmountCents() {
        return amountCents;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getAmountDollars() {
        return BigDecimal.valueOf(amountCents)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}