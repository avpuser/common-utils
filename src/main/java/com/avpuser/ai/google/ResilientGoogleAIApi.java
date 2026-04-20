package com.avpuser.ai.google;

import com.avpuser.ai.AIModel;
import com.avpuser.ai.AIProvider;
import com.avpuser.ai.AiApiException;
import com.avpuser.ai.AiErrorType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ResilientGoogleAIApi extends GoogleAIApi {

    private static final Logger logger = LogManager.getLogger(ResilientGoogleAIApi.class);

    private static final Duration RATE_LIMIT_COOLDOWN = Duration.ofMinutes(20);
    private static final Duration QUOTA_EXCEEDED_COOLDOWN = Duration.ofHours(1);

    private final List<GoogleAIApi> delegates;
    private final Clock clock;
    private final ConcurrentHashMap<GoogleAIApi, Instant> cooldownUntil = new ConcurrentHashMap<>();
    private final AtomicInteger nextRoundRobinIndex = new AtomicInteger(0);

    public ResilientGoogleAIApi(List<String> apiKeys) {
        super(firstKey(requireNonEmptyKeys(apiKeys)));
        this.delegates = apiKeys.stream().map(GoogleAIApi::new).toList();
        this.clock = Clock.systemUTC();
    }

    ResilientGoogleAIApi(List<GoogleAIApi> delegates, Clock clock) {
        super("unused-resilient-google-ai-api-parent-key");
        if (delegates == null || delegates.isEmpty()) {
            throw new IllegalArgumentException("delegates must not be null or empty");
        }
        this.delegates = List.copyOf(delegates);
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    private static List<String> requireNonEmptyKeys(List<String> apiKeys) {
        if (apiKeys == null || apiKeys.isEmpty()) {
            throw new IllegalArgumentException("apiKeys must not be null or empty");
        }
        return apiKeys;
    }

    private static String firstKey(List<String> apiKeys) {
        return apiKeys.get(0);
    }

    @Override
    public String execCompletions(String userPrompt, String systemPrompt, AIModel model) {
        return executeWithRotation(api -> api.execCompletions(userPrompt, systemPrompt, model));
    }

    @Override
    public String extractTextFromFile(byte[] fileBytes, String mimeType, String prompt) {
        return executeWithRotation(api -> api.extractTextFromFile(fileBytes, mimeType, prompt));
    }

    @Override
    public AIProvider aiProvider() {
        return delegates.get(0).aiProvider();
    }

    @FunctionalInterface
    private interface ApiCall {
        String call(GoogleAIApi api);
    }

    private String executeWithRotation(ApiCall apiCall) {
        Instant now = clock.instant();
        List<Integer> indices = availableIndicesInRotationOrder(now);
        if (indices.isEmpty()) {
            logAllDelegatesUnavailable(now);
            throw allKeysUnavailableException();
        }
        for (int idx : indices) {
            GoogleAIApi api = delegates.get(idx);
            try {
                String result = apiCall.call(api);
                advanceRotationAfterSuccess(idx);
                logger.info("Request succeeded on delegate {} of {}", idx, delegates.size());
                return result;
            } catch (AiApiException e) {
                if (e.getErrorType() == AiErrorType.RATE_LIMIT) {
                    putOnCooldown(api, RATE_LIMIT_COOLDOWN, AiErrorType.RATE_LIMIT);
                } else if (e.getErrorType() == AiErrorType.QUOTA_EXCEEDED) {
                    putOnCooldown(api, QUOTA_EXCEEDED_COOLDOWN, AiErrorType.QUOTA_EXCEEDED);
                } else {
                    throw e;
                }
            }
        }
        logAllDelegatesUnavailable(clock.instant());
        throw allKeysUnavailableException();
    }

    private AiApiException allKeysUnavailableException() {
        return new AiApiException(
                429,
                "All Google AI API keys are temporarily unavailable",
                AIProvider.GOOGLE,
                AiErrorType.QUOTA_EXCEEDED
        );
    }

    private void advanceRotationAfterSuccess(int usedIndex) {
        int n = delegates.size();
        nextRoundRobinIndex.set((usedIndex + 1) % n);
    }

    private List<Integer> availableIndicesInRotationOrder(Instant now) {
        int n = delegates.size();
        int start = Math.floorMod(nextRoundRobinIndex.get(), n);
        List<Integer> ordered = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            int idx = (start + k) % n;
            if (!isCoolingDown(delegates.get(idx), now)) {
                ordered.add(idx);
            }
        }
        return ordered;
    }

    private boolean isCoolingDown(GoogleAIApi api, Instant now) {
        Instant until = cooldownUntil.get(api);
        if (until == null) {
            return false;
        }
        if (now.isBefore(until)) {
            return true;
        }
        cooldownUntil.remove(api, until);
        return false;
    }

    private void putOnCooldown(GoogleAIApi api, Duration duration, AiErrorType attemptedReason) {
        Instant newUntil = clock.instant().plus(duration);
        Instant effectiveUntil = cooldownUntil.compute(api, (k, oldUntil) ->
                oldUntil == null || newUntil.isAfter(oldUntil) ? newUntil : oldUntil);
        int idx = delegates.indexOf(api);
        int unavailable = countDelegatesInCooldown(clock.instant());
        String reasonLabel = attemptedReason == AiErrorType.RATE_LIMIT
                ? "rate-limited"
                : (attemptedReason == AiErrorType.QUOTA_EXCEEDED ? "quota-exceeded" : "cooldown");
        logger.info("Delegate {} {}, cooldown until {} (unavailable delegates: {}/{})",
                idx, reasonLabel, effectiveUntil, unavailable, delegates.size());
    }

    private int countDelegatesInCooldown(Instant now) {
        int n = 0;
        for (GoogleAIApi d : delegates) {
            if (isCoolingDown(d, now)) {
                n++;
            }
        }
        return n;
    }

    private void logAllDelegatesUnavailable(Instant now) {
        int unavailable = countDelegatesInCooldown(now);
        logger.info("All delegates unavailable (in cooldown: {}/{}); failing with QUOTA_EXCEEDED",
                unavailable, delegates.size());
    }

    /**
     * For unit tests: applies the same cooldown merge rules as after API errors, without an HTTP call.
     */
    void applyCooldownForTests(GoogleAIApi delegate, Duration duration, AiErrorType attemptedReason) {
        putOnCooldown(delegate, duration, attemptedReason);
    }

    /**
     * For unit tests: number of entries in the cooldown map (including expired entries not yet removed).
     */
    int cooldownMapSizeForTests() {
        return cooldownUntil.size();
    }

    /**
     * For unit tests: current stored cooldown end instant for a delegate, or null if none.
     */
    Instant peekCooldownUntilForTests(GoogleAIApi delegate) {
        return cooldownUntil.get(delegate);
    }
}
