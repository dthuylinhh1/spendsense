package com.example.demo.transaction;

import java.time.LocalDate;
import java.math.BigDecimal;

public class TransactionResponse {

    private Long id;
    private LocalDate transDate;
    private LocalDate postedDate;
    private String description;
    private String bankCategory;
    private String cardRef;
    private long amountCents;
    private BigDecimal amount;
    private String currency;
    private String source;

    public TransactionResponse() {
    }

    public TransactionResponse(
            Long id,
            LocalDate transDate,
            LocalDate postedDate,
            String description,
            String bankCategory,
            String cardRef,
            long amountCents,
            String currency,
            String source
    ) {
        this.id = id;
        this.transDate = transDate;
        this.postedDate = postedDate;
        this.description = description;
        this.bankCategory = bankCategory;
        this.cardRef = cardRef;
        this.amountCents = amountCents;
        this.amount = BigDecimal.valueOf(amountCents, 2);
        this.currency = currency;
        this.source = source;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getTransDate() {
        return transDate;
    }

    public LocalDate getPostedDate() {
        return postedDate;
    }

    public String getDescription() {
        return description;
    }

    public String getBankCategory() {
        return bankCategory;
    }

    public String getCardRef() {
        return cardRef;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getSource() {
        return source;
    }
}