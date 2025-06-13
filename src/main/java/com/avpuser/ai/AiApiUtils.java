package com.avpuser.ai;

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

        if (AiResponseParser.isResponseCutOff(body)) {
            logger.error("Response from AI is cut off.");
        }

        logger.info("Response body: {}", body);
        return body;
    }

    public static void checkAndThrowIfError(HttpResponse<String> response, AIProvider providerName) {
        int status = response.statusCode();
        String body = response.body();

        if (status >= 200 && status < 300) {
            return;
        }

        String message = extractApiErrorMessage(body);
        logger.error("{} API error. Status: {}, Message: {}", providerName.name(), status, message);
        throw new AiApiException(status, message);
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
            logger.warn("Failed to parse error body: {}", body);
        }
        return "Unknown API error";
    }

}
