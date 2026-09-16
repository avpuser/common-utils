package com.avpuser.ai.metrics;

import com.avpuser.ai.AIModel;
import com.avpuser.ai.AIProvider;
import com.avpuser.ai.AiApiException;
import com.avpuser.ai.AiErrorType;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

/**
 * Closed-set label helpers for AI metrics (no high-cardinality values).
 */
public final class AiMetricsTags {

    public static final String OUTCOME_SUCCESS = "success";
    public static final String OUTCOME_FINAL_FAILURE = "final_failure";

    public static final String RECOVERED_TRUE = "true";
    public static final String RECOVERED_FALSE = "false";

    public static final String RESULT_SUCCESS = "success";
    public static final String RESULT_ERROR = "error";

    public static final String ERROR_TYPE_NONE = "none";
    public static final String ACCOUNT_NONE = "none";
    public static final String FINAL_MODEL_NONE = "none";
    public static final String PROMPT_TYPE_UNKNOWN = "unknown";

    private AiMetricsTags() {
    }

    public static String promptType(String promptType) {
        if (promptType == null || promptType.isBlank()) {
            return PROMPT_TYPE_UNKNOWN;
        }
        return promptType;
    }

    public static String model(AIModel model) {
        return model == null ? "unknown" : model.name();
    }

    public static String provider(AIProvider provider) {
        return provider == null ? "unknown" : provider.name();
    }

    public static String account(int index) {
        return "g" + index;
    }

    public static String errorType(AiErrorType errorType) {
        if (errorType == null) {
            return AiErrorType.UNKNOWN.name().toLowerCase(Locale.ROOT);
        }
        return errorType.name().toLowerCase(Locale.ROOT);
    }

    public static String errorType(Throwable throwable) {
        if (throwable == null) {
            return errorType(AiErrorType.UNKNOWN);
        }
        int aiIdx = ExceptionUtils.indexOfType(throwable, AiApiException.class);
        if (aiIdx != -1) {
            Throwable[] chain = ExceptionUtils.getThrowables(throwable);
            return errorType(((AiApiException) chain[aiIdx]).getErrorType());
        }
        if (ExceptionUtils.indexOfType(throwable, TimeoutException.class) != -1) {
            return errorType(AiErrorType.TEMPORARY_UNAVAILABLE);
        }
        if (ExceptionUtils.indexOfType(throwable, IOException.class) != -1) {
            return errorType(AiErrorType.NETWORK_ERROR);
        }
        return errorType(AiErrorType.UNKNOWN);
    }
}
