package com.avpuser.ai;

import com.avpuser.ai.executor.AiResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculator for AI API costs based on token usage.
 * Calculates cost using input tokens and combined output/reasoning tokens.
 */
public class AiCostCalculator {

    private static final BigDecimal MILLION = new BigDecimal("1000000");
    private static final int SCALE = 10; // Decimal precision for calculations

    /**
     * Calculates the total cost for an AI response.
     * 
     * @param aiResponse The AI response containing token counts and model information
     * @return The total cost as BigDecimal, or BigDecimal.ZERO if model is null or all tokens are null
     */
    public static BigDecimal calculateCost(AiResponse aiResponse) {
        if (aiResponse == null || aiResponse.getModel() == null) {
            return BigDecimal.ZERO;
        }

        AIModel model = aiResponse.getModel();
        BigDecimal inputPricePerMillion = model.getInputPricePerMillionTokens();
        BigDecimal outputPricePerMillion = model.getOutputPricePerMillionTokens();

        // Calculate input cost
        Integer inputTokens = aiResponse.getInputTokens();
        BigDecimal inputCost = calculateTokenCost(inputTokens, inputPricePerMillion);

        // Calculate output cost (outputTokens + reasoningTokens)
        Integer outputTokens = aiResponse.getOutputTokens();
        Integer reasoningTokens = aiResponse.getReasoningTokens();
        int totalOutputTokens = (outputTokens != null ? outputTokens : 0) + 
                                (reasoningTokens != null ? reasoningTokens : 0);
        BigDecimal outputCost = calculateTokenCost(totalOutputTokens, outputPricePerMillion);

        return inputCost.add(outputCost).stripTrailingZeros();
    }

    /**
     * Calculates cost for a given number of tokens and price per million tokens.
     * 
     * @param tokens Number of tokens (can be null, treated as 0)
     * @param pricePerMillion Price per million tokens
     * @return Cost as BigDecimal
     */
    private static BigDecimal calculateTokenCost(Integer tokens, BigDecimal pricePerMillion) {
        if (tokens == null || tokens == 0 || pricePerMillion == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal tokensDecimal = new BigDecimal(tokens);
        return tokensDecimal
                .multiply(pricePerMillion)
                .divide(MILLION, SCALE, RoundingMode.HALF_UP);
    }
}

