package com.avpuser.ai.executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Simple retrying executor:
 * - Iterates over steps produced by {@link DefaultRetryPolicy#stepsFor(AiPromptRequest)}:
 * current model first, then fallback models.
 * - Executes exactly one attempt per step (no backoff, no repeats on the same model).
 * - If all steps are exhausted, throws a RuntimeException with the last error as a cause.
 */
public class RetryAiExecutor implements AiExecutor {

    private static final Logger logger = LogManager.getLogger(RetryAiExecutor.class);

    private final AiExecutor delegate;
    private final DefaultRetryPolicy retryPolicy;

    public RetryAiExecutor(AiExecutor delegate, DefaultRetryPolicy retryPolicy) {
        this.delegate = delegate;
        this.retryPolicy = retryPolicy;
    }

    /**
     * Wraps any Throwable into a RuntimeException, preserving the original cause/message.
     * If the Throwable is already a RuntimeException, returns it as-is (optionally
     * enriching the message).
     */
    private static RuntimeException wrap(Throwable t, String contextMessage) {
        if (t instanceof RuntimeException re) {
            // Keep original runtime; enrich message if context provided.
            if (contextMessage == null || contextMessage.isBlank()) return re;
            return new RuntimeException(contextMessage, re);
        }
        String msg = (contextMessage == null || contextMessage.isBlank())
                ? t.getMessage()
                : contextMessage;
        return new RuntimeException(msg, t);
    }

    @Override
    public AiResponse execute(AiPromptRequest originalRequest) {
        Throwable lastError = null;

        for (AiPromptRequest promptRequest : retryPolicy.stepsFor(originalRequest)) {
            try {
                logger.info("Executing AI request: model={}, promptType={}",
                        promptRequest.getModel(), promptRequest.getPromptType());
                return delegate.execute(promptRequest);
            } catch (Throwable t) {
                lastError = t;

                final boolean retryable;
                try {
                    retryable = retryPolicy.isRetryable(t);
                } catch (Throwable policyError) {
                    throw wrap(policyError, "Retry policy evaluation failed");
                }


                if (retryable) {
                    logger.error("Retryable failure on model={}", promptRequest.getModel(), t);
                } else {
                    logger.error("Non-AI error on model={} — stopping retries. cause={}",
                            promptRequest.getModel(), t.toString(), t);
                    throw wrap(t, "Non-retryable failure");
                }
            }
        }

        // All steps exhausted -> throw runtime with the last error as cause (if present).
        if (lastError == null) {
            throw new RuntimeException("All retry steps exhausted");
        }
        throw wrap(lastError, "All retry steps exhausted");
    }

}