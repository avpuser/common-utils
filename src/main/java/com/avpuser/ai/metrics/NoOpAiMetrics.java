package com.avpuser.ai.metrics;

/**
 * No-op {@link AiMetrics} for tests and constructors without a MeterRegistry.
 */
public final class NoOpAiMetrics implements AiMetrics {

    public static final NoOpAiMetrics INSTANCE = new NoOpAiMetrics();

    private static final RequestTimer NOOP_TIMER = (promptType, primaryModel, finalModel, outcome, recovered) -> {
    };

    private NoOpAiMetrics() {
    }

    @Override
    public RequestTimer startRequestTimer() {
        return NOOP_TIMER;
    }

    @Override
    public void recordRequest(String promptType,
                              String primaryModel,
                              String finalModel,
                              String outcome,
                              String recovered) {
    }

    @Override
    public void recordModelFallback(String promptType,
                                    String fromModel,
                                    String toModel,
                                    String reason) {
    }

    @Override
    public void recordAttempt(String provider,
                              String model,
                              String account,
                              String result,
                              String errorType) {
    }
}
