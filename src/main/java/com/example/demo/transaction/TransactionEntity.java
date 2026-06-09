package com.example.demo.transaction;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "account_id", nullable = false)
  private Long accountId;

  @Column(name = "trans_date")
  private LocalDate transDate;

  @Column(name = "posted_date", nullable = false)
  private LocalDate postedDate;

  @Column(name = "description", nullable = false)
  private String description;

  @Column(name = "bank_category")
  private String bankCategory;

  @Column(name = "card_ref")
  private String cardRef;

  @Column(name = "amount_cents", nullable = false)
  private long amountCents;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency = "CAD";

  @Column(name = "source", nullable = false)
  private String source = "CIBC";

  @Column(name = "import_hash", nullable = false)
  private String importHash;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }

  public Long getAccountId() { return accountId; }
  public void setAccountId(Long accountId) { this.accountId = accountId; }

  public LocalDate getTransDate() { return transDate; }
  public void setTransDate(LocalDate transDate) { this.transDate = transDate; }

  public LocalDate getPostedDate() { return postedDate; }
  public void setPostedDate(LocalDate postedDate) { this.postedDate = postedDate; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public String getBankCategory() { return bankCategory; }
  public void setBankCategory(String bankCategory) { this.bankCategory = bankCategory; }

  public String getCardRef() { return cardRef; }
  public void setCardRef(String cardRef) { this.cardRef = cardRef; }

  public long getAmountCents() { return amountCents; }
  public void setAmountCents(long amountCents) { this.amountCents = amountCents; }

  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }

  public String getSource() { return source; }
  public void setSource(String source) { this.source = source; }

  public String getImportHash() { return importHash; }
  public void setImportHash(String importHash) { this.importHash = importHash; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
