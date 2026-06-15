package com.example.demo.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CycleOnlyTransactionRow {

    private final String date;
    private final String description;
    private final String cardRef;
    private final String category;
    private final Long amountCents;

    public CycleOnlyTransactionRow(
            String date,
            String description,
            String cardRef,
            String category,
            Long amountCents
    ) {
        this.date = date;
        this.description = description;
        this.cardRef = cardRef;
        this.category = category;
        this.amountCents = amountCents;
    }

    public String getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public String getCardRef() {
        return cardRef;
    }

    public String getCategory() {
        return category;
    }

    public Long getAmountCents() {
        return amountCents;
    }

    public BigDecimal getAmountDollars() {
        return BigDecimal.valueOf(amountCents)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}