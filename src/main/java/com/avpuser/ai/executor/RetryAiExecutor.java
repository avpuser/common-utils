package com.avpuser.ai.executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Simple retrying executor:
 * - Iterates over steps produced by {@link DefaultRetryPolicy#stepsFor(AiPromptRequest)}:
 *   current model first, then fallback models.
 * - Executes exactly one attempt per step (no backoff, no repeats on the same model).
 * - On a failure, checks {@link DefaultRetryPolicy#isRetryable(Throwable)}:
 *   if retryable -> proceeds to the next step; otherwise -> fails immediately.
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

    @Override
    public AiResponse execute(AiPromptRequest originalRequest) {
        Throwable lastError = null;

        for (AiPromptRequest stepReq : retryPolicy.stepsFor(originalRequest)) {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Executing AI request: model={}, promptType={}",
                            stepReq.getModel(), stepReq.getPromptType());
                }
                return delegate.execute(stepReq);
            } catch (Throwable t) {
                lastError = t;
                final boolean retryable;
                try {
                    retryable = retryPolicy.isRetryable(t);
                } catch (Throwable policyError) {
                    // If policy itself fails, propagate as runtime immediately.
                    throw wrap(policyError, "Retry policy evaluation failed");
                }

                if (!retryable) {
                    // Non-retryable -> fail fast.
                    throw wrap(t, "Non-retryable failure");
                }

                // Retryable -> log and proceed to the next fallback step (if any).
                logger.warn("Retryable failure on model={} (type={}). Will try next step if any. cause={}",
                        stepReq.getModel(), stepReq.getPromptType(), t.toString());
            }
        }

        // All steps exhausted -> throw runtime with the last error as cause (if present).
        if (lastError == null) {
            throw new RuntimeException("All retry steps exhausted");
        }
        throw wrap(lastError, "All retry steps exhausted");
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
}