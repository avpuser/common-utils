package com.avpuser.ai;

import com.avpuser.utils.LogSanitizerUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AiApiUtils {

    private static final Logger logger = LogManager.getLogger(AiApiUtils.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String handleResponse(HttpResponse<String> response, AIProvider aiProvider) {
        checkAndThrowIfError(response, aiProvider);
        String body = response.body();

        if (OpenAiCompatibleResponseParser.isResponseCutOff(body)) {
            logger.error("Response from AI is cut off.");
        }

        logger.info("AI response body: length={}", body != null ? body.length() : 0);
        return body;
    }

    public static void checkAndThrowIfError(HttpResponse<String> response, AIProvider aiProvider) {
        int status = response.statusCode();
        String body = response.body();

        if (status >= 200 && status < 300) {
            return;
        }

        String message = extractApiErrorMessage(body);
        AiErrorType errorType = classifyError(status, message);
        logger.error("{} API error. Status: {}, Message: {}", aiProvider.name(), status, LogSanitizerUtils.sanitizeExceptionMessage(message));
        throw new AiApiException(status, message, aiProvider, errorType);
    }

    private static AiErrorType classifyError(int status, String message) {
        String msg = message == null ? "" : message.toLowerCase();

        if (status == 429) {
            if (msg.contains("quota")
                    || msg.contains("exceeded your current quota")
                    || msg.contains("requests per day")
                    || msg.contains("rpd")) {
                return AiErrorType.QUOTA_EXCEEDED;
            }
            if (msg.contains("too many requests")
                    || msg.contains("rate limit")
                    || msg.contains("requests per minute")
                    || msg.contains("rpm")) {
                return AiErrorType.RATE_LIMIT;
            }
            return AiErrorType.RATE_LIMIT;
        }

        if (status == 401) {
            return AiErrorType.AUTH_ERROR;
        }

        if (status == 403) {
            if (msg.contains("safety")
                    || msg.contains("policy")
                    || msg.contains("blocked")) {
                return AiErrorType.CONTENT_BLOCKED;
            }
            return AiErrorType.PERMISSION_DENIED;
        }

        if (status == 404) {
            return AiErrorType.NOT_FOUND;
        }

        if (status == 400) {
            if (msg.contains("safety")
                    || msg.contains("policy")
                    || msg.contains("blocked")) {
                return AiErrorType.CONTENT_BLOCKED;
            }
            return AiErrorType.INVALID_REQUEST;
        }

        if (status == 503
                || msg.contains("temporarily unavailable")
                || msg.contains("overloaded")
                || msg.contains("try again later")) {
            return AiErrorType.TEMPORARY_UNAVAILABLE;
        }

        if (status >= 500) {
            return AiErrorType.SERVER_ERROR;
        }

        return AiErrorType.UNKNOWN;
    }

    public static List<Map<String, Object>> createMessages(String userPrompt, String systemPrompt) {
        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        // Сообщение с ролью user
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);

        return messages;
    }

    @SuppressWarnings("unchecked")
    public static String extractApiErrorMessage(String body) {
        try {
            Map<String, Object> parsed = mapper.readValue(body, Map.class);
            Map<String, Object> error = (Map<String, Object>) parsed.get("error");
            if (error != null) {
                return (String) error.getOrDefault("message", "Unknown error");
            }
        } catch (Exception e) {
            logger.warn("Failed to parse error body: length={}", body != null ? body.length() : 0);
        }
        return "Unknown API error";
    }

}
