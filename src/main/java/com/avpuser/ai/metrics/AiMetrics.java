package com.avpuser.ai.metrics;

/**
 * Thin observability hooks for AI/LLM calls. Implementations must not affect control flow.
 */
public interface AiMetrics {

    /**
     * Starts a timer for one logical AI request ({@code RetryAiExecutor} scope).
     * Call {@link RequestTimer#stop} exactly once on every exit path.
     */
    RequestTimer startRequestTimer();

    void recordRequest(String promptType,
                       String primaryModel,
                       String finalModel,
                       String outcome,
                       String recovered);

    void recordModelFallback(String promptType,
                             String fromModel,
                             String toModel,
                             String reason);

    void recordAttempt(String provider,
                       String model,
                       String account,
                       String result,
                       String errorType);

    /**
     * Stops a logical-request timer with the same tags as {@link #recordRequest}.
     */
    interface RequestTimer {
        void stop(String promptType,
                  String primaryModel,
                  String finalModel,
                  String outcome,
                  String recovered);
    }
}
