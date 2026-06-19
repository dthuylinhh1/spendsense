package com.example.demo.transaction;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiCycleInsightService {

    private static final int MAX_ROW_COUNT = 6;
    private static final String DEVELOPER_PROMPT = """
            You are SpendSense's finance insight writer.
            Give concise, practical observations from statement-cycle data.
            Do not claim certainty beyond the data provided.
            Do not give investment, tax, legal, or credit advice.
            Return 4 short bullet points and one short next-step sentence.
            """;
    private static final int ESTIMATED_OUTPUT_TOKENS = 220;

    private final RestClient restClient;
    private final AiCycleInsightRepository aiCycleInsightRepository;
    private final String apiKey;
    private final String model;
    private final BigDecimal inputCostPerMillionTokens;
    private final BigDecimal outputCostPerMillionTokens;
    private final Map<String, String> insightCache = new ConcurrentHashMap<>();

    public AiCycleInsightService(
            AiCycleInsightRepository aiCycleInsightRepository,
            @Value("${openai.api-key:${OPENAI_API_KEY:}}") String apiKey,
            @Value("${openai.model:gpt-5.4-mini}") String model,
            @Value("${openai.pricing.input-per-million-usd:0.75}") BigDecimal inputCostPerMillionTokens,
            @Value("${openai.pricing.output-per-million-usd:4.50}") BigDecimal outputCostPerMillionTokens
    ) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .build();
        this.aiCycleInsightRepository = aiCycleInsightRepository;
        this.apiKey = apiKey;
        this.model = model;
        this.inputCostPerMillionTokens = inputCostPerMillionTokens;
        this.outputCostPerMillionTokens = outputCostPerMillionTokens;
    }

    public AiCycleInsightCostEstimate estimateCost(
            BigDecimal cycleATotalDollars,
            BigDecimal cycleBTotalDollars,
            BigDecimal differenceDollars,
            BigDecimal recurringTotalDollars,
            BigDecimal cycleAOnlyTotalDollars,
            BigDecimal cycleBOnlyTotalDollars,
            List<CategoryComparisonRow> categoryComparisonRows,
            List<RecurringTransactionRow> recurringTransactionRows,
            List<CycleOnlyTransactionRow> cycleAOnlyRows,
            List<CycleOnlyTransactionRow> cycleBOnlyRows
    ) {
        String userPrompt = buildPrompt(
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

        int inputTokens = estimateTokens(DEVELOPER_PROMPT + "\n" + userPrompt);
        int outputTokens = ESTIMATED_OUTPUT_TOKENS;
        BigDecimal estimatedCostUsd = estimateCostUsd(inputTokens, outputTokens);

        return new AiCycleInsightCostEstimate(
                model,
                inputTokens,
                outputTokens,
                estimatedCostUsd
        );
    }

    public boolean hasSavedInsight(Long cycleAId, Long cycleBId) {
        return aiCycleInsightRepository.existsByCycleAIdAndCycleBIdAndModel(cycleAId, cycleBId, model);
    }

    public String generateInsight(
            Long cycleAId,
            Long cycleBId,
            BigDecimal cycleATotalDollars,
            BigDecimal cycleBTotalDollars,
            BigDecimal differenceDollars,
            BigDecimal recurringTotalDollars,
            BigDecimal cycleAOnlyTotalDollars,
            BigDecimal cycleBOnlyTotalDollars,
            List<CategoryComparisonRow> categoryComparisonRows,
            List<RecurringTransactionRow> recurringTransactionRows,
            List<CycleOnlyTransactionRow> cycleAOnlyRows,
            List<CycleOnlyTransactionRow> cycleBOnlyRows
    ) {
        String cacheKey = buildCacheKey(cycleAId, cycleBId);
        String cachedInsight = insightCache.get(cacheKey);
        if (cachedInsight != null) {
            return cachedInsight;
        }

        var savedInsight = aiCycleInsightRepository
                .findByCycleAIdAndCycleBIdAndModel(cycleAId, cycleBId, model);
        if (savedInsight.isPresent()) {
            String insightText = savedInsight.get().getInsightText();
            insightCache.put(cacheKey, insightText);
            return insightText;
        }

        if (apiKey == null || apiKey.isBlank()) {
            return "AI insight is not configured yet. Set OPENAI_API_KEY, then click Generate AI Insight again.";
        }

        AiCycleInsightCostEstimate costEstimate = estimateCost(
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

        String userPrompt = buildPrompt(
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

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "reasoning", Map.of("effort", "low"),
                "input", List.of(
                        Map.of(
                                "role", "developer",
                                "content", DEVELOPER_PROMPT
                        ),
                        Map.of(
                                "role", "user",
                                "content", userPrompt
                        )
                )
        );

        JsonNode response = restClient.post()
                .uri("/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        String insight = extractOutputText(response);
        saveInsight(cycleAId, cycleBId, insight, costEstimate);
        insightCache.put(cacheKey, insight);
        return insight;
    }

    public String getModel() {
        return model;
    }

    private String buildCacheKey(Long cycleAId, Long cycleBId) {
        return cycleAId + ":" + cycleBId + ":" + model;
    }

    private void saveInsight(
            Long cycleAId,
            Long cycleBId,
            String insight,
            AiCycleInsightCostEstimate costEstimate
    ) {
        AiCycleInsightEntity entity = new AiCycleInsightEntity();
        entity.setCycleAId(cycleAId);
        entity.setCycleBId(cycleBId);
        entity.setModel(model);
        entity.setInsightText(insight);
        entity.setEstimatedInputTokens(costEstimate.estimatedInputTokens());
        entity.setEstimatedOutputTokens(costEstimate.estimatedOutputTokens());
        entity.setEstimatedCostUsd(costEstimate.estimatedCostUsd());
        entity.setCreatedAt(Instant.now());

        aiCycleInsightRepository.save(entity);
    }

    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        return (int) Math.ceil(text.length() / 4.0);
    }

    private BigDecimal estimateCostUsd(int inputTokens, int outputTokens) {
        BigDecimal inputCost = BigDecimal.valueOf(inputTokens)
                .multiply(inputCostPerMillionTokens)
                .divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);

        BigDecimal outputCost = BigDecimal.valueOf(outputTokens)
                .multiply(outputCostPerMillionTokens)
                .divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);

        return inputCost.add(outputCost).setScale(6, RoundingMode.HALF_UP);
    }

    private String buildPrompt(
            BigDecimal cycleATotalDollars,
            BigDecimal cycleBTotalDollars,
            BigDecimal differenceDollars,
            BigDecimal recurringTotalDollars,
            BigDecimal cycleAOnlyTotalDollars,
            BigDecimal cycleBOnlyTotalDollars,
            List<CategoryComparisonRow> categoryComparisonRows,
            List<RecurringTransactionRow> recurringTransactionRows,
            List<CycleOnlyTransactionRow> cycleAOnlyRows,
            List<CycleOnlyTransactionRow> cycleBOnlyRows
    ) {
        return """
                Compare these two credit-card statement cycles.

                Cycle A total: $%s
                Cycle B total: $%s
                Difference, Cycle B minus Cycle A: $%s
                Recurring/fixed total found in both cycles: $%s
                Cycle A one-time total: $%s
                Cycle B one-time total: $%s

                Largest category changes:
                %s

                Largest recurring/fixed transactions:
                %s

                Largest Cycle A-only transactions:
                %s

                Largest Cycle B-only transactions:
                %s
                """.formatted(
                cycleATotalDollars,
                cycleBTotalDollars,
                differenceDollars,
                recurringTotalDollars,
                cycleAOnlyTotalDollars,
                cycleBOnlyTotalDollars,
                summarizeCategories(categoryComparisonRows),
                summarizeRecurring(recurringTransactionRows),
                summarizeCycleOnly(cycleAOnlyRows),
                summarizeCycleOnly(cycleBOnlyRows)
        );
    }

    private String summarizeCategories(List<CategoryComparisonRow> rows) {
        if (rows.isEmpty()) {
            return "None";
        }

        return rows.stream()
                .limit(MAX_ROW_COUNT)
                .map(row -> "- %s: Cycle A $%s, Cycle B $%s, difference $%s".formatted(
                        row.getCategory(),
                        row.getCycleADollars(),
                        row.getCycleBDollars(),
                        row.getDifferenceDollars()
                ))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("None");
    }

    private String summarizeRecurring(List<RecurringTransactionRow> rows) {
        if (rows.isEmpty()) {
            return "None";
        }

        return rows.stream()
                .limit(MAX_ROW_COUNT)
                .map(row -> "- %s, %s, card %s, $%s".formatted(
                        row.getDescription(),
                        row.getCategory(),
                        row.getCardRef(),
                        row.getAmountDollars()
                ))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("None");
    }

    private String summarizeCycleOnly(List<CycleOnlyTransactionRow> rows) {
        if (rows.isEmpty()) {
            return "None";
        }

        return rows.stream()
                .limit(MAX_ROW_COUNT)
                .map(row -> "- %s, %s, %s, card %s, $%s".formatted(
                        row.getDate(),
                        row.getDescription(),
                        row.getCategory(),
                        row.getCardRef(),
                        row.getAmountDollars()
                ))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("None");
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) {
            return "AI insight could not be generated because the API returned an empty response.";
        }

        JsonNode outputText = response.path("output_text");
        if (outputText.isTextual() && !outputText.asText().isBlank()) {
            return outputText.asText();
        }

        for (JsonNode outputItem : response.path("output")) {
            for (JsonNode contentItem : outputItem.path("content")) {
                JsonNode text = contentItem.path("text");
                if (text.isTextual() && !text.asText().isBlank()) {
                    return text.asText();
                }
            }
        }

        return "AI insight could not be generated from this response.";
    }

    public record AiCycleInsightCostEstimate(
            String model,
            int estimatedInputTokens,
            int estimatedOutputTokens,
            BigDecimal estimatedCostUsd
    ) {
    }
}
