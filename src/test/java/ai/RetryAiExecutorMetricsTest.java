package ai;

import ai.metrics.RecordingAiMetrics;
import com.avpuser.ai.AIModel;
import com.avpuser.ai.AiApiException;
import com.avpuser.ai.AIProvider;
import com.avpuser.ai.AiErrorType;
import com.avpuser.ai.executor.AiExecutor;
import com.avpuser.ai.executor.AiPromptRequest;
import com.avpuser.ai.executor.AiResponse;
import com.avpuser.ai.executor.DefaultRetryPolicy;
import com.avpuser.ai.executor.RetryAiExecutor;
import com.avpuser.ai.metrics.AiMetricsTags;
import com.avpuser.test.MockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockTest
class RetryAiExecutorMetricsTest {

    private AiExecutor delegate;
    private RecordingAiMetrics metrics;
    private RetryAiExecutor retryExecutor;

    @BeforeEach
    void setUp() {
        delegate = mock(AiExecutor.class);
        metrics = new RecordingAiMetrics();
        retryExecutor = new RetryAiExecutor(delegate, new DefaultRetryPolicy(), metrics);
    }

    @Test
    void primarySuccess_recordsRequestNotRecovered_andNoFallback_andNoAttempts() {
        AiPromptRequest request = AiPromptRequest.withFallback(
                "user", "system", AIModel.GEMINI_FLASH, "tests_extraction",
                Set.of(AIModel.GPT_4O_MINI, AIModel.DEEPSEEK_CHAT));
        AiResponse ok = new AiResponse("ok", AIModel.GEMINI_FLASH);
        when(delegate.execute(any())).thenReturn(ok);

        assertSame(ok, retryExecutor.execute(request));

        assertEquals(1, metrics.requests.size());
        assertEquals(new RecordingAiMetrics.RequestEvent(
                "tests_extraction", "GEMINI_FLASH", "GEMINI_FLASH",
                AiMetricsTags.OUTCOME_SUCCESS, AiMetricsTags.RECOVERED_FALSE), metrics.requests.get(0));
        assertTrue(metrics.fallbacks.isEmpty());
        assertTrue(metrics.attempts.isEmpty());
        assertEquals(1, metrics.timersStarted.get());
        assertEquals(1, metrics.timersStopped.get());
        verify(delegate, times(1)).execute(any());
    }

    @Test
    void modelFallbackSuccess_recordsRecoveredTrue_andOneFallback_noAttempts() {
        AiPromptRequest request = AiPromptRequest.withFallback(
                "user", "system", AIModel.GEMINI_FLASH, "patient_question",
                new LinkedHashSet<>(List.of(AIModel.GPT_4O_MINI)));
        AiApiException geminiFail = new AiApiException(429, "rate", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT);
        AiResponse ok = new AiResponse("ok", AIModel.GPT_4O_MINI);
        when(delegate.execute(any()))
                .thenThrow(geminiFail)
                .thenReturn(ok);

        assertSame(ok, retryExecutor.execute(request));

        assertEquals(1, metrics.fallbacks.size());
        assertEquals(new RecordingAiMetrics.FallbackEvent(
                "patient_question", "GEMINI_FLASH", "GPT_4O_MINI", "rate_limit"), metrics.fallbacks.get(0));
        assertEquals(1, metrics.requests.size());
        assertEquals(new RecordingAiMetrics.RequestEvent(
                "patient_question", "GEMINI_FLASH", "GPT_4O_MINI",
                AiMetricsTags.OUTCOME_SUCCESS, AiMetricsTags.RECOVERED_TRUE), metrics.requests.get(0));
        assertTrue(metrics.attempts.isEmpty());
        verify(delegate, times(2)).execute(any());
    }

    @Test
    void allStepsExhausted_recordsFinalFailure_notRecovered() {
        AiPromptRequest request = AiPromptRequest.withFallback(
                "user", "system", AIModel.GEMINI_FLASH, "meta_extraction",
                new LinkedHashSet<>(List.of(AIModel.GPT_4O_MINI, AIModel.DEEPSEEK_CHAT)));
        when(delegate.execute(any())).thenThrow(
                new AiApiException(500, "boom", AIProvider.GOOGLE, AiErrorType.SERVER_ERROR));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> retryExecutor.execute(request));
        assertEquals("All retry steps exhausted", ex.getMessage());

        assertEquals(2, metrics.fallbacks.size());
        assertEquals("GEMINI_FLASH", metrics.fallbacks.get(0).fromModel());
        assertEquals("GPT_4O_MINI", metrics.fallbacks.get(0).toModel());
        assertEquals("GPT_4O_MINI", metrics.fallbacks.get(1).fromModel());
        assertEquals("DEEPSEEK_CHAT", metrics.fallbacks.get(1).toModel());
        assertEquals(1, metrics.requests.size());
        assertEquals(new RecordingAiMetrics.RequestEvent(
                "meta_extraction", "GEMINI_FLASH", AiMetricsTags.FINAL_MODEL_NONE,
                AiMetricsTags.OUTCOME_FINAL_FAILURE, AiMetricsTags.RECOVERED_FALSE), metrics.requests.get(0));
        assertTrue(metrics.attempts.isEmpty());
        verify(delegate, times(3)).execute(any());
    }

    @Test
    void nonRetryable_recordsFinalFailure_andDoesNotFallback() {
        AiPromptRequest request = AiPromptRequest.withFallback(
                "user", "system", AIModel.GEMINI_FLASH, "tests_extraction",
                Set.of(AIModel.GPT_4O_MINI));
        when(delegate.execute(any())).thenThrow(new IllegalStateException("parse"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> retryExecutor.execute(request));
        assertEquals("Non-retryable failure", ex.getMessage());

        assertTrue(metrics.fallbacks.isEmpty());
        assertEquals(1, metrics.requests.size());
        assertEquals(AiMetricsTags.OUTCOME_FINAL_FAILURE, metrics.requests.get(0).outcome());
        assertEquals(AiMetricsTags.RECOVERED_FALSE, metrics.requests.get(0).recovered());
        verify(delegate, times(1)).execute(any());
        verify(delegate, never()).execute(org.mockito.ArgumentMatchers.argThat(
                r -> r != null && r.getModel() == AIModel.GPT_4O_MINI));
    }
}
