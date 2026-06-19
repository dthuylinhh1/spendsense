package com.example.demo.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "ai_cycle_insights",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_ai_cycle_insights_pair_model",
                columnNames = {"cycle_a_id", "cycle_b_id", "model"}
        )
)
public class AiCycleInsightEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cycle_a_id", nullable = false)
    private Long cycleAId;

    @Column(name = "cycle_b_id", nullable = false)
    private Long cycleBId;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "insight_text", nullable = false, columnDefinition = "text")
    private String insightText;

    @Column(name = "estimated_input_tokens", nullable = false)
    private int estimatedInputTokens;

    @Column(name = "estimated_output_tokens", nullable = false)
    private int estimatedOutputTokens;

    @Column(name = "estimated_cost_usd", nullable = false, precision = 12, scale = 6)
    private BigDecimal estimatedCostUsd;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public Long getCycleAId() {
        return cycleAId;
    }

    public void setCycleAId(Long cycleAId) {
        this.cycleAId = cycleAId;
    }

    public Long getCycleBId() {
        return cycleBId;
    }

    public void setCycleBId(Long cycleBId) {
        this.cycleBId = cycleBId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getInsightText() {
        return insightText;
    }

    public void setInsightText(String insightText) {
        this.insightText = insightText;
    }

    public int getEstimatedInputTokens() {
        return estimatedInputTokens;
    }

    public void setEstimatedInputTokens(int estimatedInputTokens) {
        this.estimatedInputTokens = estimatedInputTokens;
    }

    public int getEstimatedOutputTokens() {
        return estimatedOutputTokens;
    }

    public void setEstimatedOutputTokens(int estimatedOutputTokens) {
        this.estimatedOutputTokens = estimatedOutputTokens;
    }

    public BigDecimal getEstimatedCostUsd() {
        return estimatedCostUsd;
    }

    public void setEstimatedCostUsd(BigDecimal estimatedCostUsd) {
        this.estimatedCostUsd = estimatedCostUsd;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
