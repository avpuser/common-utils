package ai;

import com.avpuser.ai.AIModel;
import com.avpuser.ai.AiCostCalculator;
import com.avpuser.ai.executor.AiResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AiCostCalculatorTest {

    @Test
    public void testCalculateCost_withAllTokens() {
        // Given: 1000 input tokens, 500 output tokens, 200 reasoning tokens
        // GPT_4O: input $2.50/1M, output $10.00/1M
        // Expected: (1000 * 2.50 / 1M) + ((500 + 200) * 10.00 / 1M) = 0.0025 + 0.007 = 0.0095
        AiResponse response = new AiResponse(
                "test response",
                AIModel.GPT_4O,
                1000,
                500,
                200,
                null,
                null,
                null
        );

        BigDecimal cost = AiCostCalculator.calculateCost(response);

        assertNotNull(cost);
        // Expected: 0.0025 + 0.007 = 0.0095
        assertEquals(new BigDecimal("0.0095"), cost);
    }

    @Test
    public void testCalculateCost_withInputTokensOnly() {
        // Given: 1000 input tokens, no output/reasoning tokens
        // GPT_4O_MINI: input $0.15/1M, output $0.60/1M
        // Expected: (1000 * 0.15 / 1M) + 0 = 0.00015
        AiResponse response = new AiResponse(
                "test response",
                AIModel.GPT_4O_MINI,
                1000,
                null,
                null,
                null,
                null,
                null
        );

        BigDecimal cost = AiCostCalculator.calculateCost(response);

        assertNotNull(cost);
        assertEquals(new BigDecimal("0.00015"), cost);
    }

    @Test
    public void testCalculateCost_withOutputTokensOnly() {
        // Given: 1000 output tokens, no input/reasoning tokens
        // DEEPSEEK_CHAT: input $0.27/1M, output $1.10/1M
        // Expected: 0 + (1000 * 1.10 / 1M) = 0.0011
        AiResponse response = new AiResponse(
                "test response",
                AIModel.DEEPSEEK_CHAT,
                null,
                1000,
                null,
                null,
                null,
                null
        );

        BigDecimal cost = AiCostCalculator.calculateCost(response);

        assertNotNull(cost);
        assertEquals(new BigDecimal("0.0011"), cost);
    }

    @Test
    public void testCalculateCost_withReasoningTokensOnly() {
        // Given: 1000 reasoning tokens, no input/output tokens
        // DEEPSEEK_REASONER: input $0.55/1M, output $2.19/1M
        // Expected: 0 + (1000 * 2.19 / 1M) = 0.00219
        AiResponse response = new AiResponse(
                "test response",
                AIModel.DEEPSEEK_REASONER,
                null,
                null,
                1000,
                null,
                null,
                null
        );

        BigDecimal cost = AiCostCalculator.calculateCost(response);

        assertNotNull(cost);
        assertEquals(new BigDecimal("0.00219"), cost);
    }

    @Test
    public void testCalculateCost_withOutputAndReasoningTokens() {
        // Given: 500 output tokens + 300 reasoning tokens
        // GEMINI_PRO: input $1.25/1M, output $10.00/1M
        // Expected: 0 + ((500 + 300) * 10.00 / 1M) = 0.008
        AiResponse response = new AiResponse(
                "test response",
                AIModel.GEMINI_PRO,
                null,
                500,
                300,
                null,
                null,
                null
        );

        BigDecimal cost = AiCostCalculator.calculateCost(response);

        assertNotNull(cost);
        assertEquals(new BigDecimal("0.008"), cost);
    }

    @Test
    public void testCalculateCost_withAllNullTokens() {
        // Given: all tokens are null
        // Expected: 0
        AiResponse response = new AiResponse(
                "test response",
                AIModel.GPT_4O,
                null,
                null,
                null,
                null,
                null,
                null
        );

        BigDecimal cost = AiCostCalculator.calculateCost(response);

        assertNotNull(cost);
        assertEquals(BigDecimal.ZERO, cost);
    }

    @Test
    public void testCalculateCost_withZeroTokens() {
        // Given: all tokens are 0
        // Expected: 0
        AiResponse response = new AiResponse(
                "test response",
                AIModel.GPT_4O,
                0,
                0,
                0,
                null,
                null,
                null
        );

        BigDecimal cost = AiCostCalculator.calculateCost(response);

        assertNotNull(cost);
        assertEquals(BigDecimal.ZERO, cost);
    }

    @Test
    public void testCalculateCost_withNullResponse() {
        // Given: null response
        // Expected: 0
        BigDecimal cost = AiCostCalculator.calculateCost(null);

        assertNotNull(cost);
        assertEquals(BigDecimal.ZERO, cost);
    }

    @Test
    public void testCalculateCost_withNullModel() {
        // Given: response with null model
        // Expected: 0
        AiResponse response = new AiResponse(
                "test response",
                null,
                1000,
                500,
                200,
                null,
                null,
                null
        );

        BigDecimal cost = AiCostCalculator.calculateCost(response);

        assertNotNull(cost);
        assertEquals(BigDecimal.ZERO, cost);
    }

    @Test
    public void testCalculateCost_largeTokenCounts() {
        // Given: 1M input tokens, 500K output tokens, 200K reasoning tokens
        // GPT_4O: input $2.50/1M, output $10.00/1M
        // Expected: (1M * 2.50 / 1M) + ((500K + 200K) * 10.00 / 1M) = 2.50 + 7.00 = 9.50
        AiResponse response = new AiResponse(
                "test response",
                AIModel.GPT_4O,
                1_000_000,
                500_000,
                200_000,
                null,
                null,
                null
        );

        BigDecimal cost = AiCostCalculator.calculateCost(response);

        assertNotNull(cost);
        assertEquals(new BigDecimal("9.5"), cost);
    }

    @Test
    public void testCalculateCost_withGeminiFlash() {
        // Given: 2000 input tokens, 1000 output tokens, 500 reasoning tokens
        // GEMINI_FLASH: input $0.30/1M, output $2.50/1M
        // Expected: (2000 * 0.30 / 1M) + ((1000 + 500) * 2.50 / 1M) = 0.0006 + 0.00375 = 0.00435
        AiResponse response = new AiResponse(
                "test response",
                AIModel.GEMINI_FLASH,
                2000,
                1000,
                500,
                null,
                null,
                null
        );

        BigDecimal cost = AiCostCalculator.calculateCost(response);

        assertNotNull(cost);
        assertEquals(new BigDecimal("0.00435"), cost);
    }

    @Test
    public void testCalculateCost_precision() {
        // Given: 1 input token, 1 output token
        // GPT_4O: input $2.50/1M, output $10.00/1M
        // Expected: (1 * 2.50 / 1M) + (1 * 10.00 / 1M) = 0.0000025 + 0.00001 = 0.0000125
        AiResponse response = new AiResponse(
                "test response",
                AIModel.GPT_4O,
                1,
                1,
                null,
                null,
                null,
                null
        );

        BigDecimal cost = AiCostCalculator.calculateCost(response);

        assertNotNull(cost);
        // Verify precision is maintained
        assertEquals(new BigDecimal("0.0000125"), cost);
    }
}

