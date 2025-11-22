package com.avpuser.ai.google;

import com.avpuser.ai.AIApi;
import com.avpuser.ai.AIModel;
import com.avpuser.ai.AIProvider;
import com.avpuser.ai.AiApiUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class GoogleAIApi implements AIApi {

    private static final Logger logger = LogManager.getLogger(GoogleAIApi.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final AIProvider AI_PROVIDER = AIProvider.GOOGLE;

    private final HttpClient client = HttpClient.newHttpClient();

    private final String apiKey;

    public GoogleAIApi(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String execCompletions(String userPrompt, String systemPrompt, AIModel model) {
        logger.info("Google Gemini exec: model={}, userPrompt={}", model.getModelName(), userPrompt);

        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                model.getModelName(), apiKey);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("role", "user", "parts", List.of(Map.of("text", systemPrompt + "\n" + userPrompt)))
                )
        );

        String json;
        try {
            json = mapper.writeValueAsString(requestBody);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize request body", e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            AiApiUtils.checkAndThrowIfError(response, aiProvider());
            // Return full JSON response body so usageMetadata can be extracted
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to call Gemini API", e);
        }
    }

    private String extractResponseText(String responseBody) {
        try {
            Map<?, ?> parsed = mapper.readValue(responseBody, Map.class);
            List<?> candidates = (List<?>) parsed.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<?, ?> first = (Map<?, ?>) candidates.get(0);
                Map<?, ?> content = (Map<?, ?>) first.get("content");
                List<?> parts = (List<?>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    Map<?, ?> part = (Map<?, ?>) parts.get(0);
                    return (String) part.get("text");
                }
            }
            throw new RuntimeException("Empty or unexpected Gemini response structure");
        } catch (Exception e) {
            logger.error("Failed to parse Gemini response", e);
            throw new RuntimeException("Failed to parse Gemini response", e);
        }
    }

    public String extractTextFromFile(byte[] fileBytes, String mimeType, String prompt) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("File bytes must not be empty");
        }
        if (StringUtils.isBlank(mimeType)) {
            throw new IllegalArgumentException("Prompt must not be blank");
        }

        AIModel model = AIModel.GEMINI_FLASH;
        logger.info("Google Gemini image exec: model={}, mimeType={}, promptLength={}",
                model.getModelName(), mimeType, prompt.length());

        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                model.getModelName(), apiKey);

        String jsonRequest = buildGeminiImageRequest(fileBytes, mimeType, prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            AiApiUtils.checkAndThrowIfError(response, aiProvider());
            return extractResponseText(response.body());

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to call Gemini API (file)", e);
        }
    }

    private String buildGeminiImageRequest(byte[] fileBytes, String mimeType, String prompt) {
        String base64 = Base64.getEncoder().encodeToString(fileBytes);

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of("text", prompt),
                                        Map.of("inline_data", Map.of(
                                                "mime_type", mimeType,
                                                "data", base64
                                        ))
                                )
                        )
                )
        );

        try {
            return mapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Gemini image request JSON", e);
        }
    }

    @Override
    public AIProvider aiProvider() {
        return AI_PROVIDER;
    }
}
