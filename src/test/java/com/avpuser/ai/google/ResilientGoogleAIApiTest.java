package com.avpuser.ai.google;

import com.avpuser.ai.AIModel;
import com.avpuser.ai.AIProvider;
import com.avpuser.ai.AiApiException;
import com.avpuser.ai.AiErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResilientGoogleAIApiTest {

    private static final Instant T0 = Instant.parse("2026-04-20T12:00:00Z");

    private GoogleAIApi d0;

    private GoogleAIApi d1;

    private GoogleAIApi d2;

    @BeforeEach
    void setUp() {
        d0 = mock(GoogleAIApi.class);
        d1 = mock(GoogleAIApi.class);
        d2 = mock(GoogleAIApi.class);
    }

    private static Clock mutableClock(AtomicReference<Instant> holder) {
        return new Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Instant instant() {
                return holder.get();
            }
        };
    }

    @Test
    void constructor_rejectsNullApiKeys() {
        assertThrows(IllegalArgumentException.class, () -> new ResilientGoogleAIApi(null));
    }

    @Test
    void constructor_rejectsEmptyApiKeys() {
        assertThrows(IllegalArgumentException.class, () -> new ResilientGoogleAIApi(List.of()));
    }

    @Test
    void delegatesConstructor_rejectsNullDelegates() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        assertThrows(IllegalArgumentException.class, () -> new ResilientGoogleAIApi(null, mutableClock(t)));
    }

    @Test
    void delegatesConstructor_rejectsEmptyDelegates() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        assertThrows(IllegalArgumentException.class, () -> new ResilientGoogleAIApi(List.of(), mutableClock(t)));
    }

    @Test
    void execCompletions_singleDelegate_success() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(eq("u"), eq("s"), eq(AIModel.GEMINI_FLASH))).thenReturn("ok");
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0), mutableClock(t));
        assertEquals("ok", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        verify(d0, times(1)).execCompletions("u", "s", AIModel.GEMINI_FLASH);
    }

    @Test
    void execCompletions_rateLimitThenSuccessOnSecondDelegate() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any())).thenThrow(new AiApiException(429, "too many", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT));
        when(d1.execCompletions(any(), any(), any())).thenReturn("second");
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));
        assertEquals("second", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        verify(d0, times(1)).execCompletions(any(), any(), any());
        verify(d1, times(1)).execCompletions(any(), any(), any());
    }

    @Test
    void execCompletions_quotaExceededThenSuccessOnSecondDelegate() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any())).thenThrow(new AiApiException(429, "quota", AIProvider.GOOGLE, AiErrorType.QUOTA_EXCEEDED));
        when(d1.execCompletions(any(), any(), any())).thenReturn("ok");
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));
        assertEquals("ok", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        verify(d1, times(1)).execCompletions(any(), any(), any());
    }

    @Test
    void execCompletions_nonRetryableAiErrorPropagatesImmediately() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any())).thenThrow(new AiApiException(401, "auth", AIProvider.GOOGLE, AiErrorType.AUTH_ERROR));
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));
        AiApiException ex = assertThrows(AiApiException.class, () -> api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertEquals(AiErrorType.AUTH_ERROR, ex.getErrorType());
        verify(d1, never()).execCompletions(any(), any(), any());
    }

    @Test
    void execCompletions_runtimeExceptionPropagates() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any())).thenThrow(new IllegalStateException("boom"));
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));
        assertThrows(IllegalStateException.class, () -> api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        verify(d1, never()).execCompletions(any(), any(), any());
    }

    @Test
    void execCompletions_allDelegatesRateLimitedInOneRound_throwsQuotaExceeded() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any())).thenThrow(new AiApiException(429, "r0", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT));
        when(d1.execCompletions(any(), any(), any())).thenThrow(new AiApiException(429, "r1", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT));
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));
        AiApiException ex = assertThrows(AiApiException.class, () -> api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertEquals(AiErrorType.QUOTA_EXCEEDED, ex.getErrorType());
    }

    @Test
    void execCompletions_skipsDelegateStillInCooldownFromRateLimit() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any()))
                .thenThrow(new AiApiException(429, "r", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT))
                .thenReturn("recovered");
        when(d1.execCompletions(any(), any(), any())).thenReturn("from1");
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));
        assertEquals("from1", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        t.set(T0.plusSeconds(1));
        assertEquals("from1", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        t.set(T0.plus(Duration.ofMinutes(21)));
        assertEquals("recovered", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        verify(d0, times(2)).execCompletions(any(), any(), any());
        verify(d1, times(2)).execCompletions(any(), any(), any());
    }

    @Test
    void execCompletions_skipsDelegateInQuotaCooldownLongerWindow() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any()))
                .thenThrow(new AiApiException(429, "q", AIProvider.GOOGLE, AiErrorType.QUOTA_EXCEEDED))
                .thenReturn("from0");
        when(d1.execCompletions(any(), any(), any())).thenReturn("from1");
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));
        assertEquals("from1", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        t.set(T0.plus(Duration.ofMinutes(30)));
        assertEquals("from1", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        t.set(T0.plus(Duration.ofMinutes(61)));
        assertEquals("from0", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
    }

    @Test
    void execCompletions_allInCooldownBeforeCall_throwsQuotaExceeded() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any())).thenThrow(new AiApiException(429, "r", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT));
        when(d1.execCompletions(any(), any(), any())).thenThrow(new AiApiException(429, "r", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT));
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));
        assertThrows(AiApiException.class, () -> api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        AiApiException ex = assertThrows(AiApiException.class, () -> api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertEquals(AiErrorType.QUOTA_EXCEEDED, ex.getErrorType());
    }

    @Test
    void execCompletions_roundRobinAdvancesAfterSuccess() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any())).thenReturn("a");
        when(d1.execCompletions(any(), any(), any())).thenReturn("b");
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));
        assertEquals("a", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertEquals("b", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertEquals("a", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        verify(d0, times(2)).execCompletions(any(), any(), any());
        verify(d1, times(1)).execCompletions(any(), any(), any());
    }

    @Test
    void execCompletions_threeDelegates_rotationOrder() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any())).thenReturn("0");
        when(d1.execCompletions(any(), any(), any())).thenReturn("1");
        when(d2.execCompletions(any(), any(), any())).thenReturn("2");
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));
        assertEquals("0", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertEquals("1", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertEquals("2", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertEquals("0", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
    }

    @Test
    void extractTextFromFile_roundRobinAfterSuccess() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        byte[] bytes = new byte[]{9};
        when(d0.extractTextFromFile(eq(bytes), eq("image/png"), eq("p"))).thenReturn("a");
        when(d1.extractTextFromFile(eq(bytes), eq("image/png"), eq("p"))).thenReturn("b");
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));
        assertEquals("a", api.extractTextFromFile(bytes, "image/png", "p"));
        assertEquals("b", api.extractTextFromFile(bytes, "image/png", "p"));
        assertEquals("a", api.extractTextFromFile(bytes, "image/png", "p"));
        verify(d0, times(2)).extractTextFromFile(bytes, "image/png", "p");
        verify(d1, times(1)).extractTextFromFile(bytes, "image/png", "p");
    }

    @Test
    void extractTextFromFile_delegatesWithSameCooldownRules() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        byte[] bytes = new byte[]{1, 2, 3};
        when(d0.extractTextFromFile(eq(bytes), eq("image/png"), eq("p"))).thenThrow(new AiApiException(429, "r", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT));
        when(d1.extractTextFromFile(eq(bytes), eq("image/png"), eq("p"))).thenReturn("body");
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));
        assertEquals("body", api.extractTextFromFile(bytes, "image/png", "p"));
        verify(d0).extractTextFromFile(bytes, "image/png", "p");
        verify(d1).extractTextFromFile(bytes, "image/png", "p");
    }

    @Test
    void extractTextFromFile_allQuotaExceeded_throwsQuotaExceeded() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        byte[] bytes = new byte[]{1};
        when(d0.extractTextFromFile(any(), any(), any())).thenThrow(new AiApiException(429, "q", AIProvider.GOOGLE, AiErrorType.QUOTA_EXCEEDED));
        when(d1.extractTextFromFile(any(), any(), any())).thenThrow(new AiApiException(429, "q", AIProvider.GOOGLE, AiErrorType.QUOTA_EXCEEDED));
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));
        AiApiException ex = assertThrows(AiApiException.class, () -> api.extractTextFromFile(bytes, "image/png", "p"));
        assertEquals(AiErrorType.QUOTA_EXCEEDED, ex.getErrorType());
    }

    @Test
    void aiProvider_matchesFirstDelegate() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.aiProvider()).thenReturn(AIProvider.GOOGLE);
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));
        assertEquals(AIProvider.GOOGLE, api.aiProvider());
    }

    @Test
    void execCompletions_rateLimitCooldownEndsAfterTwentyMinutes() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any()))
                .thenThrow(new AiApiException(429, "r", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT))
                .thenReturn("ok0");
        when(d1.execCompletions(any(), any(), any())).thenReturn("ok1");
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));
        assertEquals("ok1", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        t.set(T0.plus(Duration.ofMinutes(19)).plus(Duration.ofSeconds(59)));
        assertEquals("ok1", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        t.set(T0.plus(Duration.ofMinutes(20)));
        assertEquals("ok0", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
    }

    @Test
    void execCompletions_mixedRateLimitAndQuota_stillTriesRemainingUntilExhausted() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any())).thenThrow(new AiApiException(429, "r", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT));
        when(d1.execCompletions(any(), any(), any())).thenThrow(new AiApiException(429, "q", AIProvider.GOOGLE, AiErrorType.QUOTA_EXCEEDED));
        when(d2.execCompletions(any(), any(), any())).thenReturn("ok");
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));
        assertEquals("ok", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        verify(d0, times(1)).execCompletions(any(), any(), any());
        verify(d1, times(1)).execCompletions(any(), any(), any());
        verify(d2, times(1)).execCompletions(any(), any(), any());
    }

    @Test
    void cooldownMerge_quotaThenShorterRateLimit_keepsOneHourEnd() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));
        api.applyCooldownForTests(d0, Duration.ofHours(1), AiErrorType.QUOTA_EXCEEDED);
        assertEquals(T0.plus(Duration.ofHours(1)), api.peekCooldownUntilForTests(d0));
        t.set(T0.plusSeconds(1));
        api.applyCooldownForTests(d0, Duration.ofMinutes(20), AiErrorType.RATE_LIMIT);
        assertEquals(T0.plus(Duration.ofHours(1)), api.peekCooldownUntilForTests(d0));
    }

    @Test
    void cooldownMerge_rateLimitThenQuota_extendsToLongerEnd() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));
        api.applyCooldownForTests(d0, Duration.ofMinutes(20), AiErrorType.RATE_LIMIT);
        assertEquals(T0.plus(Duration.ofMinutes(20)), api.peekCooldownUntilForTests(d0));
        t.set(T0.plusSeconds(1));
        api.applyCooldownForTests(d0, Duration.ofHours(1), AiErrorType.QUOTA_EXCEEDED);
        assertEquals(T0.plusSeconds(1).plus(Duration.ofHours(1)), api.peekCooldownUntilForTests(d0));
    }

    @Test
    void execCompletions_quotaCooldownEndsAfterOneHour() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any()))
                .thenThrow(new AiApiException(429, "q", AIProvider.GOOGLE, AiErrorType.QUOTA_EXCEEDED))
                .thenReturn("ok0");
        when(d1.execCompletions(any(), any(), any())).thenReturn("ok1");
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));
        assertEquals("ok1", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        t.set(T0.plus(Duration.ofHours(1)).minus(Duration.ofSeconds(1)));
        assertEquals("ok1", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        t.set(T0.plus(Duration.ofHours(1)));
        assertEquals("ok0", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
    }

    @Test
    void execCompletions_expiredCooldownEntryRemovedFromMapWhenChecked() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any()))
                .thenThrow(new AiApiException(429, "r", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT))
                .thenReturn("ok0");
        when(d1.execCompletions(any(), any(), any())).thenReturn("ok1");
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));
        assertEquals("ok1", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertEquals(1, api.cooldownMapSizeForTests());
        t.set(T0.plus(Duration.ofMinutes(21)));
        assertEquals("ok0", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertEquals(0, api.cooldownMapSizeForTests());
        assertNull(api.peekCooldownUntilForTests(d0));
    }

    @Test
    void execCompletions_mixedRateQuotaRateAllExhausted_throwsQuotaExceeded() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any())).thenThrow(new AiApiException(429, "r0", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT));
        when(d1.execCompletions(any(), any(), any())).thenThrow(new AiApiException(429, "q", AIProvider.GOOGLE, AiErrorType.QUOTA_EXCEEDED));
        when(d2.execCompletions(any(), any(), any())).thenThrow(new AiApiException(429, "r2", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT));
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));
        AiApiException ex = assertThrows(AiApiException.class, () -> api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertEquals(AiErrorType.QUOTA_EXCEEDED, ex.getErrorType());
        verify(d0, times(1)).execCompletions(any(), any(), any());
        verify(d1, times(1)).execCompletions(any(), any(), any());
        verify(d2, times(1)).execCompletions(any(), any(), any());
    }

    @Test
    void stress_concurrentExecCompletions_noDeadlockOrConsistentFailures() throws Exception {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        AtomicInteger invocation = new AtomicInteger();
        when(d0.execCompletions(any(), any(), any())).thenAnswer(inv -> {
            int n = invocation.incrementAndGet();
            if (n % 17 == 0) {
                throw new AiApiException(429, "r", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT);
            }
            return "ok";
        });
        when(d1.execCompletions(any(), any(), any())).thenReturn("ok");
        when(d2.execCompletions(any(), any(), any())).thenReturn("ok");
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));
        int threads = 40;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                for (int j = 0; j < 50; j++) {
                    try {
                        api.execCompletions("u", "s", AIModel.GEMINI_FLASH);
                    } catch (AiApiException e) {
                        assertEquals(AiErrorType.QUOTA_EXCEEDED, e.getErrorType());
                    }
                }
                return null;
            });
        }
        List<Future<Void>> futures = pool.invokeAll(tasks, 120, TimeUnit.SECONDS);
        for (Future<Void> f : futures) {
            f.get();
        }
        pool.shutdownNow();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void execCompletions_whenAllDelegatesAlreadyInCooldown_doesNotCallAnyDelegate() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));

        api.applyCooldownForTests(d0, Duration.ofMinutes(20), AiErrorType.RATE_LIMIT);
        api.applyCooldownForTests(d1, Duration.ofHours(1), AiErrorType.QUOTA_EXCEEDED);
        api.applyCooldownForTests(d2, Duration.ofMinutes(20), AiErrorType.RATE_LIMIT);

        AiApiException ex = assertThrows(AiApiException.class,
                () -> api.execCompletions("u", "s", AIModel.GEMINI_FLASH));

        assertEquals(AiErrorType.QUOTA_EXCEEDED, ex.getErrorType());
        verify(d0, never()).execCompletions(any(), any(), any());
        verify(d1, never()).execCompletions(any(), any(), any());
        verify(d2, never()).execCompletions(any(), any(), any());
    }

    @Test
    void extractTextFromFile_whenAllDelegatesAlreadyInCooldown_doesNotCallAnyDelegate() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        byte[] bytes = new byte[]{4, 5, 6};
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));

        api.applyCooldownForTests(d0, Duration.ofMinutes(20), AiErrorType.RATE_LIMIT);
        api.applyCooldownForTests(d1, Duration.ofHours(1), AiErrorType.QUOTA_EXCEEDED);
        api.applyCooldownForTests(d2, Duration.ofMinutes(20), AiErrorType.RATE_LIMIT);

        AiApiException ex = assertThrows(AiApiException.class,
                () -> api.extractTextFromFile(bytes, "image/png", "p"));

        assertEquals(AiErrorType.QUOTA_EXCEEDED, ex.getErrorType());
        verify(d0, never()).extractTextFromFile(any(), any(), any());
        verify(d1, never()).extractTextFromFile(any(), any(), any());
        verify(d2, never()).extractTextFromFile(any(), any(), any());
    }

    @Test
    void execCompletions_afterRateLimitOnFirstAndSuccessOnSecond_nextCallStartsFromThird() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any()))
                .thenThrow(new AiApiException(429, "r", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT));
        when(d1.execCompletions(any(), any(), any())).thenReturn("from1");
        when(d2.execCompletions(any(), any(), any())).thenReturn("from2");

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));

        assertEquals("from1", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertEquals("from2", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));

        verify(d0, times(1)).execCompletions(any(), any(), any());
        verify(d1, times(1)).execCompletions(any(), any(), any());
        verify(d2, times(1)).execCompletions(any(), any(), any());
    }

    @Test
    void execCompletions_skipsMiddleDelegateInCooldown_andUsesNextAvailable() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any())).thenReturn("from0");
        when(d2.execCompletions(any(), any(), any())).thenReturn("from2");

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));
        api.applyCooldownForTests(d1, Duration.ofMinutes(20), AiErrorType.RATE_LIMIT);

        assertEquals("from0", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertEquals("from2", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));

        verify(d0, times(1)).execCompletions(any(), any(), any());
        verify(d1, never()).execCompletions(any(), any(), any());
        verify(d2, times(1)).execCompletions(any(), any(), any());
    }

    @Test
    void execCompletions_expiredCooldownOnOneDelegate_doesNotRemoveOtherActiveCooldowns() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any())).thenReturn("from0");
        when(d1.execCompletions(any(), any(), any())).thenReturn("from1");

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));
        api.applyCooldownForTests(d0, Duration.ofMinutes(20), AiErrorType.RATE_LIMIT);
        api.applyCooldownForTests(d2, Duration.ofHours(1), AiErrorType.QUOTA_EXCEEDED);

        t.set(T0.plus(Duration.ofMinutes(21)));

        assertEquals("from0", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertNull(api.peekCooldownUntilForTests(d0));
        assertEquals(T0.plus(Duration.ofHours(1)), api.peekCooldownUntilForTests(d2));
        assertEquals(1, api.cooldownMapSizeForTests());
    }

    @Test
    void extractTextFromFile_nonRetryableAiErrorPropagatesImmediately() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        byte[] bytes = new byte[]{7, 8};
        when(d0.extractTextFromFile(any(), any(), any()))
                .thenThrow(new AiApiException(401, "auth", AIProvider.GOOGLE, AiErrorType.AUTH_ERROR));

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));

        AiApiException ex = assertThrows(AiApiException.class,
                () -> api.extractTextFromFile(bytes, "image/png", "p"));

        assertEquals(AiErrorType.AUTH_ERROR, ex.getErrorType());
        verify(d0, times(1)).extractTextFromFile(any(), any(), any());
        verify(d1, never()).extractTextFromFile(any(), any(), any());
    }

    @Test
    void extractTextFromFile_runtimeExceptionPropagatesImmediately() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        byte[] bytes = new byte[]{7, 8};
        when(d0.extractTextFromFile(any(), any(), any()))
                .thenThrow(new IllegalStateException("boom"));

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));

        assertThrows(IllegalStateException.class,
                () -> api.extractTextFromFile(bytes, "image/png", "p"));

        verify(d0, times(1)).extractTextFromFile(any(), any(), any());
        verify(d1, never()).extractTextFromFile(any(), any(), any());
    }

    @Test
    void extractTextFromFile_allDelegatesRateLimitedInOneRound_throwsQuotaExceeded() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        byte[] bytes = new byte[]{3};
        when(d0.extractTextFromFile(any(), any(), any()))
                .thenThrow(new AiApiException(429, "r0", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT));
        when(d1.extractTextFromFile(any(), any(), any()))
                .thenThrow(new AiApiException(429, "r1", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT));

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));

        AiApiException ex = assertThrows(AiApiException.class,
                () -> api.extractTextFromFile(bytes, "image/png", "p"));

        assertEquals(AiErrorType.QUOTA_EXCEEDED, ex.getErrorType());
        verify(d0, times(1)).extractTextFromFile(any(), any(), any());
        verify(d1, times(1)).extractTextFromFile(any(), any(), any());
    }

    @Test
    void extractTextFromFile_skipsDelegateStillInCooldownFromRateLimit() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        byte[] bytes = new byte[]{1, 2};
        when(d0.extractTextFromFile(any(), any(), any()))
                .thenThrow(new AiApiException(429, "r", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT))
                .thenReturn("recovered");
        when(d1.extractTextFromFile(any(), any(), any())).thenReturn("from1");

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));

        assertEquals("from1", api.extractTextFromFile(bytes, "image/png", "p"));
        t.set(T0.plusSeconds(1));
        assertEquals("from1", api.extractTextFromFile(bytes, "image/png", "p"));
        t.set(T0.plus(Duration.ofMinutes(21)));
        assertEquals("recovered", api.extractTextFromFile(bytes, "image/png", "p"));

        verify(d0, times(2)).extractTextFromFile(any(), any(), any());
        verify(d1, times(2)).extractTextFromFile(any(), any(), any());
    }

    @Test
    void extractTextFromFile_quotaCooldownEndsAfterOneHour() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        byte[] bytes = new byte[]{9, 9};
        when(d0.extractTextFromFile(any(), any(), any()))
                .thenThrow(new AiApiException(429, "q", AIProvider.GOOGLE, AiErrorType.QUOTA_EXCEEDED))
                .thenReturn("ok0");
        when(d1.extractTextFromFile(any(), any(), any())).thenReturn("ok1");

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));

        assertEquals("ok1", api.extractTextFromFile(bytes, "image/png", "p"));
        t.set(T0.plus(Duration.ofHours(1)).minus(Duration.ofSeconds(1)));
        assertEquals("ok1", api.extractTextFromFile(bytes, "image/png", "p"));
        t.set(T0.plus(Duration.ofHours(1)));
        assertEquals("ok0", api.extractTextFromFile(bytes, "image/png", "p"));
    }

    @Test
    void extractTextFromFile_afterSuccessOnSecondDelegate_nextCallStartsFromThird() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        byte[] bytes = new byte[]{2, 4};
        when(d0.extractTextFromFile(any(), any(), any()))
                .thenThrow(new AiApiException(429, "r", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT));
        when(d1.extractTextFromFile(any(), any(), any())).thenReturn("from1");
        when(d2.extractTextFromFile(any(), any(), any())).thenReturn("from2");

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));

        assertEquals("from1", api.extractTextFromFile(bytes, "image/png", "p"));
        assertEquals("from2", api.extractTextFromFile(bytes, "image/png", "p"));

        verify(d0, times(1)).extractTextFromFile(any(), any(), any());
        verify(d1, times(1)).extractTextFromFile(any(), any(), any());
        verify(d2, times(1)).extractTextFromFile(any(), any(), any());
    }

    @Test
    void execCompletions_singleDelegateRateLimited_thenImmediateNextCallDoesNotInvokeDelegateAgain() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any()))
                .thenThrow(new AiApiException(429, "r", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT));

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0), mutableClock(t));

        assertThrows(AiApiException.class, () -> api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertThrows(AiApiException.class, () -> api.execCompletions("u", "s", AIModel.GEMINI_FLASH));

        verify(d0, times(1)).execCompletions(any(), any(), any());
    }

    @Test
    void extractTextFromFile_singleDelegateQuotaExceeded_thenImmediateNextCallDoesNotInvokeDelegateAgain() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        byte[] bytes = new byte[]{5};
        when(d0.extractTextFromFile(any(), any(), any()))
                .thenThrow(new AiApiException(429, "q", AIProvider.GOOGLE, AiErrorType.QUOTA_EXCEEDED));

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0), mutableClock(t));

        assertThrows(AiApiException.class, () -> api.extractTextFromFile(bytes, "image/png", "p"));
        assertThrows(AiApiException.class, () -> api.extractTextFromFile(bytes, "image/png", "p"));

        verify(d0, times(1)).extractTextFromFile(any(), any(), any());
    }

    @Test
    void execCompletions_concurrentCooldownMerge_keepsLongestDeadline() throws Exception {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<?> f1 = pool.submit(() -> api.applyCooldownForTests(d0, Duration.ofHours(1), AiErrorType.QUOTA_EXCEEDED));
        Future<?> f2 = pool.submit(() -> api.applyCooldownForTests(d0, Duration.ofMinutes(20), AiErrorType.RATE_LIMIT));

        f1.get();
        f2.get();
        pool.shutdownNow();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(T0.plus(Duration.ofHours(1)), api.peekCooldownUntilForTests(d0));
    }

    @Test
    void extractTextFromFile_concurrentCooldownMerge_keepsLongestDeadline() throws Exception {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<?> f1 = pool.submit(() -> api.applyCooldownForTests(d0, Duration.ofMinutes(20), AiErrorType.RATE_LIMIT));
        Future<?> f2 = pool.submit(() -> api.applyCooldownForTests(d0, Duration.ofHours(1), AiErrorType.QUOTA_EXCEEDED));

        f1.get();
        f2.get();
        pool.shutdownNow();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        Instant actual = api.peekCooldownUntilForTests(d0);
        assertTrue(actual.equals(T0.plus(Duration.ofHours(1))) || actual.isAfter(T0.plus(Duration.ofHours(1)).minusMillis(1)));
    }

    @Test
    void execCompletions_successOnThirdDelegate_thenNextCallWrapsToFirst() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any())).thenReturn("from0");
        when(d1.execCompletions(any(), any(), any())).thenReturn("from1");
        when(d2.execCompletions(any(), any(), any())).thenReturn("from2");

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));

        assertEquals("from0", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertEquals("from1", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertEquals("from2", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertEquals("from0", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));

        verify(d0, times(2)).execCompletions(any(), any(), any());
        verify(d1, times(1)).execCompletions(any(), any(), any());
        verify(d2, times(1)).execCompletions(any(), any(), any());
    }

    @Test
    void extractTextFromFile_successOnThirdDelegate_thenNextCallWrapsToFirst() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        byte[] bytes = new byte[]{8, 8};
        when(d0.extractTextFromFile(any(), any(), any())).thenReturn("from0");
        when(d1.extractTextFromFile(any(), any(), any())).thenReturn("from1");
        when(d2.extractTextFromFile(any(), any(), any())).thenReturn("from2");

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));

        assertEquals("from0", api.extractTextFromFile(bytes, "image/png", "p"));
        assertEquals("from1", api.extractTextFromFile(bytes, "image/png", "p"));
        assertEquals("from2", api.extractTextFromFile(bytes, "image/png", "p"));
        assertEquals("from0", api.extractTextFromFile(bytes, "image/png", "p"));

        verify(d0, times(2)).extractTextFromFile(any(), any(), any());
        verify(d1, times(1)).extractTextFromFile(any(), any(), any());
        verify(d2, times(1)).extractTextFromFile(any(), any(), any());
    }

    @Test
    void execCompletions_whenFirstDelegateInManualCooldown_startsFromSecond() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d1.execCompletions(any(), any(), any())).thenReturn("from1");
        when(d2.execCompletions(any(), any(), any())).thenReturn("from2");

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));
        api.applyCooldownForTests(d0, Duration.ofMinutes(20), AiErrorType.RATE_LIMIT);

        assertEquals("from1", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertEquals("from2", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));

        verify(d0, never()).execCompletions(any(), any(), any());
        verify(d1, times(1)).execCompletions(any(), any(), any());
        verify(d2, times(1)).execCompletions(any(), any(), any());
    }

    @Test
    void extractTextFromFile_whenFirstDelegateInManualCooldown_startsFromSecond() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        byte[] bytes = new byte[]{4, 4};
        when(d1.extractTextFromFile(any(), any(), any())).thenReturn("from1");
        when(d2.extractTextFromFile(any(), any(), any())).thenReturn("from2");

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));
        api.applyCooldownForTests(d0, Duration.ofMinutes(20), AiErrorType.RATE_LIMIT);

        assertEquals("from1", api.extractTextFromFile(bytes, "image/png", "p"));
        assertEquals("from2", api.extractTextFromFile(bytes, "image/png", "p"));

        verify(d0, never()).extractTextFromFile(any(), any(), any());
        verify(d1, times(1)).extractTextFromFile(any(), any(), any());
        verify(d2, times(1)).extractTextFromFile(any(), any(), any());
    }

    @Test
    void execCompletions_expiredCooldownIsRemovedAndDelegateBecomesUsableAgain() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any())).thenReturn("from0");
        when(d1.execCompletions(any(), any(), any())).thenReturn("from1");

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));
        api.applyCooldownForTests(d0, Duration.ofMinutes(20), AiErrorType.RATE_LIMIT);

        t.set(T0.plus(Duration.ofMinutes(21)));

        assertEquals("from0", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertNull(api.peekCooldownUntilForTests(d0));
        assertEquals(0, api.cooldownMapSizeForTests());
    }

    @Test
    void extractTextFromFile_expiredCooldownIsRemovedAndDelegateBecomesUsableAgain() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        byte[] bytes = new byte[]{2, 2};

        when(d0.extractTextFromFile(any(), any(), any())).thenReturn("from0");
        when(d1.extractTextFromFile(any(), any(), any())).thenReturn("from1");

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));
        api.applyCooldownForTests(d0, Duration.ofMinutes(20), AiErrorType.RATE_LIMIT);

        t.set(T0.plus(Duration.ofMinutes(21)));

        assertEquals("from0", api.extractTextFromFile(bytes, "image/png", "p"));
        assertNull(api.peekCooldownUntilForTests(d0));
        assertEquals(0, api.cooldownMapSizeForTests());
    }

    @Test
    void execCompletions_mixedRetryableThenNonRetryable_propagatesNonRetryable() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any()))
                .thenThrow(new AiApiException(429, "r", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT));
        when(d1.execCompletions(any(), any(), any()))
                .thenThrow(new AiApiException(401, "auth", AIProvider.GOOGLE, AiErrorType.AUTH_ERROR));

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));

        AiApiException ex = assertThrows(AiApiException.class,
                () -> api.execCompletions("u", "s", AIModel.GEMINI_FLASH));

        assertEquals(AiErrorType.AUTH_ERROR, ex.getErrorType());
        verify(d0, times(1)).execCompletions(any(), any(), any());
        verify(d1, times(1)).execCompletions(any(), any(), any());
        verify(d2, never()).execCompletions(any(), any(), any());
    }

    @Test
    void extractTextFromFile_mixedRetryableThenNonRetryable_propagatesNonRetryable() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        byte[] bytes = new byte[]{1, 9};
        when(d0.extractTextFromFile(any(), any(), any()))
                .thenThrow(new AiApiException(429, "r", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT));
        when(d1.extractTextFromFile(any(), any(), any()))
                .thenThrow(new AiApiException(401, "auth", AIProvider.GOOGLE, AiErrorType.AUTH_ERROR));

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));

        AiApiException ex = assertThrows(AiApiException.class,
                () -> api.extractTextFromFile(bytes, "image/png", "p"));

        assertEquals(AiErrorType.AUTH_ERROR, ex.getErrorType());
        verify(d0, times(1)).extractTextFromFile(any(), any(), any());
        verify(d1, times(1)).extractTextFromFile(any(), any(), any());
        verify(d2, never()).extractTextFromFile(any(), any(), any());
    }

    @Test
    void execCompletions_mixedRetryableThenRuntimeException_propagatesRuntimeException() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any()))
                .thenThrow(new AiApiException(429, "r", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT));
        when(d1.execCompletions(any(), any(), any()))
                .thenThrow(new IllegalStateException("boom"));

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));

        assertThrows(IllegalStateException.class,
                () -> api.execCompletions("u", "s", AIModel.GEMINI_FLASH));

        verify(d0, times(1)).execCompletions(any(), any(), any());
        verify(d1, times(1)).execCompletions(any(), any(), any());
        verify(d2, never()).execCompletions(any(), any(), any());
    }

    @Test
    void extractTextFromFile_mixedRetryableThenRuntimeException_propagatesRuntimeException() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        byte[] bytes = new byte[]{6, 6};
        when(d0.extractTextFromFile(any(), any(), any()))
                .thenThrow(new AiApiException(429, "r", AIProvider.GOOGLE, AiErrorType.RATE_LIMIT));
        when(d1.extractTextFromFile(any(), any(), any()))
                .thenThrow(new IllegalStateException("boom"));

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));

        assertThrows(IllegalStateException.class,
                () -> api.extractTextFromFile(bytes, "image/png", "p"));

        verify(d0, times(1)).extractTextFromFile(any(), any(), any());
        verify(d1, times(1)).extractTextFromFile(any(), any(), any());
        verify(d2, never()).extractTextFromFile(any(), any(), any());
    }

    @Test
    void execCompletions_whenOnlyOneDelegateAvailable_itIsUsedRepeatedly() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d1.execCompletions(any(), any(), any())).thenReturn("from1");

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));
        api.applyCooldownForTests(d0, Duration.ofMinutes(20), AiErrorType.RATE_LIMIT);
        api.applyCooldownForTests(d2, Duration.ofHours(1), AiErrorType.QUOTA_EXCEEDED);

        assertEquals("from1", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertEquals("from1", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        assertEquals("from1", api.execCompletions("u", "s", AIModel.GEMINI_FLASH));

        verify(d0, never()).execCompletions(any(), any(), any());
        verify(d1, times(3)).execCompletions(any(), any(), any());
        verify(d2, never()).execCompletions(any(), any(), any());
    }

    @Test
    void extractTextFromFile_whenOnlyOneDelegateAvailable_itIsUsedRepeatedly() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        byte[] bytes = new byte[]{3, 3};
        when(d1.extractTextFromFile(any(), any(), any())).thenReturn("from1");

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));
        api.applyCooldownForTests(d0, Duration.ofMinutes(20), AiErrorType.RATE_LIMIT);
        api.applyCooldownForTests(d2, Duration.ofHours(1), AiErrorType.QUOTA_EXCEEDED);

        assertEquals("from1", api.extractTextFromFile(bytes, "image/png", "p"));
        assertEquals("from1", api.extractTextFromFile(bytes, "image/png", "p"));
        assertEquals("from1", api.extractTextFromFile(bytes, "image/png", "p"));

        verify(d0, never()).extractTextFromFile(any(), any(), any());
        verify(d1, times(3)).extractTextFromFile(any(), any(), any());
        verify(d2, never()).extractTextFromFile(any(), any(), any());
    }

    @Test
    void execCompletions_concurrentCalls_allSuccessfulWithHealthyDelegates() throws Exception {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        when(d0.execCompletions(any(), any(), any())).thenReturn("ok");
        when(d1.execCompletions(any(), any(), any())).thenReturn("ok");
        when(d2.execCompletions(any(), any(), any())).thenReturn("ok");

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Callable<String>> tasks = new ArrayList<>();
        for (int i = 0; i < threads * 10; i++) {
            tasks.add(() -> api.execCompletions("u", "s", AIModel.GEMINI_FLASH));
        }

        List<Future<String>> futures = pool.invokeAll(tasks, 60, TimeUnit.SECONDS);
        for (Future<String> future : futures) {
            assertEquals("ok", future.get());
        }

        pool.shutdownNow();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void extractTextFromFile_concurrentCalls_allSuccessfulWithHealthyDelegates() throws Exception {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        byte[] bytes = new byte[]{1, 1, 1};
        when(d0.extractTextFromFile(any(), any(), any())).thenReturn("ok");
        when(d1.extractTextFromFile(any(), any(), any())).thenReturn("ok");
        when(d2.extractTextFromFile(any(), any(), any())).thenReturn("ok");

        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Callable<String>> tasks = new ArrayList<>();
        for (int i = 0; i < threads * 10; i++) {
            tasks.add(() -> api.extractTextFromFile(bytes, "image/png", "p"));
        }

        List<Future<String>> futures = pool.invokeAll(tasks, 60, TimeUnit.SECONDS);
        for (Future<String> future : futures) {
            assertEquals("ok", future.get());
        }

        pool.shutdownNow();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void cooldownMerge_sameDurationAppliedTwice_keepsSingleExpectedDeadline() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));

        api.applyCooldownForTests(d0, Duration.ofMinutes(20), AiErrorType.RATE_LIMIT);
        Instant first = api.peekCooldownUntilForTests(d0);

        api.applyCooldownForTests(d0, Duration.ofMinutes(20), AiErrorType.RATE_LIMIT);
        Instant second = api.peekCooldownUntilForTests(d0);

        assertEquals(first, second);
        assertEquals(T0.plus(Duration.ofMinutes(20)), second);
    }

    @Test
    void cooldownMerge_shorterAppliedMuchLater_doesNotShortenExistingLongerDeadline() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1), mutableClock(t));

        api.applyCooldownForTests(d0, Duration.ofHours(1), AiErrorType.QUOTA_EXCEEDED);
        Instant longDeadline = api.peekCooldownUntilForTests(d0);

        t.set(T0.plus(Duration.ofMinutes(10)));
        api.applyCooldownForTests(d0, Duration.ofMinutes(20), AiErrorType.RATE_LIMIT);

        assertEquals(longDeadline, api.peekCooldownUntilForTests(d0));
    }

    @Test
    void cooldownMapSizeForTests_countsEntriesForDifferentDelegates() {
        AtomicReference<Instant> t = new AtomicReference<>(T0);
        ResilientGoogleAIApi api = new ResilientGoogleAIApi(List.of(d0, d1, d2), mutableClock(t));

        assertEquals(0, api.cooldownMapSizeForTests());

        api.applyCooldownForTests(d0, Duration.ofMinutes(20), AiErrorType.RATE_LIMIT);
        assertEquals(1, api.cooldownMapSizeForTests());

        api.applyCooldownForTests(d1, Duration.ofHours(1), AiErrorType.QUOTA_EXCEEDED);
        assertEquals(2, api.cooldownMapSizeForTests());

        api.applyCooldownForTests(d2, Duration.ofMinutes(20), AiErrorType.RATE_LIMIT);
        assertEquals(3, api.cooldownMapSizeForTests());
    }
}
