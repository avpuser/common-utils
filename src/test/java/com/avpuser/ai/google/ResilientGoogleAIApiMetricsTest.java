package com.avpuser.ai.google;

import ai.metrics.RecordingAiMetrics;
import com.avpuser.ai.AIModel;
import com.avpuser.ai.AIProvider;
import com.avpuser.ai.AiApiException;
import com.avpuser.ai.AiErrorType;
import com.avpuser.ai.metrics.AiMetricsTags;
import com.avpuser.test.MockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockTest
class ResilientGoogleAIApiMetricsTest {

    private GoogleAIApi g0;
    private GoogleAIApi g1;
    private RecordingAiMetrics metrics;
    private Clock clock;
    private ResilientGoogleAIApi api;

    @BeforeEach
    void setUp() {
        g0 = mock(GoogleAIApi.class);
        g1 = mock(GoogleAIApi.class);
        metrics = new RecordingAiMetrics();
        clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        api = new ResilientGoogleAIApi(List.of(g0, g1), clock, metrics);
    }

    @Test
    void g0Success_recordsSingleAttempt() {
        when(g0.execCompletions(anyString(), anyString(), eq(AIModel.GEMINI_FLASH))).thenReturn("{}");

        api.execCompletions("u", "s", AIModel.GEMINI_FLASH);

        assertEquals(1, metrics.attempts.size());
        assertEquals(new RecordingAiMetrics.AttemptEvent(
                "GOOGLE", "GEMINI_FLASH", "g0",
                AiMetricsTags.RESULT_SUCCESS, AiMetricsTags.ERROR_TYPE_NONE), metrics.attempts.get(0));
        verify(g0, times(1)).execCompletions(anyString(), anyString(), eq(AIModel.GEMINI_FLASH));
        verify(g1, times(0)).execCompletions(anyString(), anyString(), any());
    }

    @Test
    void g0RateLimitThenG1Success_recordsTwoAttempts_noExtraUnavailableAttempt() {
        when(g0.execCompletions(anyString(), anyString(), eq(AIModel.GEMINI_FLASH)))
                .thenThrow(new AiApiException(429, "rpm", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT));
        when(g1.execCompletions(anyString(), anyString(), eq(AIModel.GEMINI_FLASH))).thenReturn("{}");

        api.execCompletions("u", "s", AIModel.GEMINI_FLASH);

        assertEquals(2, metrics.attempts.size());
        assertEquals(new RecordingAiMetrics.AttemptEvent(
                "GOOGLE", "GEMINI_FLASH", "g0",
                AiMetricsTags.RESULT_ERROR, "rate_limit"), metrics.attempts.get(0));
        assertEquals(new RecordingAiMetrics.AttemptEvent(
                "GOOGLE", "GEMINI_FLASH", "g1",
                AiMetricsTags.RESULT_SUCCESS, AiMetricsTags.ERROR_TYPE_NONE), metrics.attempts.get(1));
    }

    @Test
    void allKeysInCooldown_recordsZeroAttempts() {
        api.applyCooldownForTests(g0, Duration.ofHours(1), AiErrorType.QUOTA_EXCEEDED);
        api.applyCooldownForTests(g1, Duration.ofHours(1), AiErrorType.QUOTA_EXCEEDED);

        AiApiException ex = assertThrows(AiApiException.class,
                () -> api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertEquals(AiErrorType.QUOTA_EXCEEDED, ex.getErrorType());
        assertTrue(metrics.attempts.isEmpty());
    }

    @Test
    void allDelegatesFailRateLimit_recordsOneAttemptPerHttpCall_notForFinalUnavailable() {
        when(g0.execCompletions(anyString(), anyString(), eq(AIModel.GEMINI_FLASH)))
                .thenThrow(new AiApiException(429, "rpm", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT));
        when(g1.execCompletions(anyString(), anyString(), eq(AIModel.GEMINI_FLASH)))
                .thenThrow(new AiApiException(429, "rpm", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT));

        AiApiException ex = assertThrows(AiApiException.class,
                () -> api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertEquals(AiErrorType.QUOTA_EXCEEDED, ex.getErrorType());
        assertEquals(2, metrics.attempts.size());
        assertEquals("g0", metrics.attempts.get(0).account());
        assertEquals("g1", metrics.attempts.get(1).account());
        assertEquals(AiMetricsTags.RESULT_ERROR, metrics.attempts.get(0).result());
        assertEquals(AiMetricsTags.RESULT_ERROR, metrics.attempts.get(1).result());
    }

    @Test
    void networkFailure_recordsAttemptThenPropagates() {
        when(g0.execCompletions(anyString(), anyString(), eq(AIModel.GEMINI_FLASH)))
                .thenThrow(new RuntimeException("Failed to call Gemini API", new java.io.IOException("boom")));

        assertThrows(RuntimeException.class, () -> api.execCompletions("u", "s", AIModel.GEMINI_FLASH));

        assertEquals(1, metrics.attempts.size());
        assertEquals("g0", metrics.attempts.get(0).account());
        assertEquals(AiMetricsTags.RESULT_ERROR, metrics.attempts.get(0).result());
        assertEquals("network_error", metrics.attempts.get(0).errorType());
        verify(g1, times(0)).execCompletions(anyString(), anyString(), any());
    }
}
