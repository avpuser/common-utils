package com.avpuser.ai.executor;

import com.avpuser.ai.AIModel;
import com.avpuser.ai.metrics.AiMetrics;
import com.avpuser.ai.metrics.AiMetricsTags;
import com.avpuser.ai.metrics.NoOpAiMetrics;
import com.avpuser.utils.LogSanitizerUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private final AiMetrics aiMetrics;

    public RetryAiExecutor(AiExecutor delegate, DefaultRetryPolicy retryPolicy) {
        this(delegate, retryPolicy, NoOpAiMetrics.INSTANCE);
    }

    public RetryAiExecutor(AiExecutor delegate, DefaultRetryPolicy retryPolicy, AiMetrics aiMetrics) {
        this.delegate = delegate;
        this.retryPolicy = retryPolicy;
        this.aiMetrics = aiMetrics == null ? NoOpAiMetrics.INSTANCE : aiMetrics;
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
        Objects.requireNonNull(originalRequest, "originalRequest");
        AiMetrics.RequestTimer timer = aiMetrics.startRequestTimer();
        String promptType = AiMetricsTags.promptType(originalRequest.getPromptType());
        String primaryModel = AiMetricsTags.model(originalRequest.getModel());
        Throwable lastError = null;

        List<AiPromptRequest> steps = new ArrayList<>();
        for (AiPromptRequest step : retryPolicy.stepsFor(originalRequest)) {
            steps.add(step);
        }

        for (int i = 0; i < steps.size(); i++) {
            AiPromptRequest promptRequest = steps.get(i);
            try {
                logger.info("Executing AI request: model={}, promptType={}",
                        promptRequest.getModel(), promptRequest.getPromptType());
                AiResponse response = delegate.execute(promptRequest);
                boolean recovered = !Objects.equals(promptRequest.getModel(), originalRequest.getModel());
                String finalModel = AiMetricsTags.model(promptRequest.getModel());
                String recoveredTag = recovered ? AiMetricsTags.RECOVERED_TRUE : AiMetricsTags.RECOVERED_FALSE;
                recordFinished(timer, promptType, primaryModel, finalModel,
                        AiMetricsTags.OUTCOME_SUCCESS, recoveredTag);
                return response;
            } catch (Throwable t) {
                lastError = t;

                final boolean retryable;
                try {
                    retryable = retryPolicy.isRetryable(t);
                } catch (Throwable policyError) {
                    recordFinished(timer, promptType, primaryModel, AiMetricsTags.FINAL_MODEL_NONE,
                            AiMetricsTags.OUTCOME_FINAL_FAILURE, AiMetricsTags.RECOVERED_FALSE);
                    throw wrap(policyError, "Retry policy evaluation failed");
                }

                if (retryable) {
                    logger.error("Retryable failure on model={}", promptRequest.getModel(), t);
                    if (i + 1 < steps.size()) {
                        AIModel toModel = steps.get(i + 1).getModel();
                        aiMetrics.recordModelFallback(
                                promptType,
                                AiMetricsTags.model(promptRequest.getModel()),
                                AiMetricsTags.model(toModel),
                                AiMetricsTags.errorType(t));
                    }
                } else {
                    logger.error("Non-AI error on model={} — stopping retries. cause={}",
                            promptRequest.getModel(), LogSanitizerUtils.sanitizeCause(t), t);
                    recordFinished(timer, promptType, primaryModel, AiMetricsTags.FINAL_MODEL_NONE,
                            AiMetricsTags.OUTCOME_FINAL_FAILURE, AiMetricsTags.RECOVERED_FALSE);
                    throw wrap(t, "Non-retryable failure");
                }
            }
        }

        recordFinished(timer, promptType, primaryModel, AiMetricsTags.FINAL_MODEL_NONE,
                AiMetricsTags.OUTCOME_FINAL_FAILURE, AiMetricsTags.RECOVERED_FALSE);
        if (lastError == null) {
            throw new RuntimeException("All retry steps exhausted");
        }
        throw wrap(lastError, "All retry steps exhausted");
    }

    private void recordFinished(AiMetrics.RequestTimer timer,
                                String promptType,
                                String primaryModel,
                                String finalModel,
                                String outcome,
                                String recovered) {
        aiMetrics.recordRequest(promptType, primaryModel, finalModel, outcome, recovered);
        timer.stop(promptType, primaryModel, finalModel, outcome, recovered);
    }
}
