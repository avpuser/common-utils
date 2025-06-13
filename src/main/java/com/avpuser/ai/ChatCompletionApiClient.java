package com.avpuser.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A default implementation of {@link AIApi} that handles chat completion requests
 * to a given AI provider's API endpoint (e.g., OpenAI, DeepSeek).
 *
 * <p>This client sends HTTP POST requests with model and message data,
 * and parses the response body while handling API errors.
 *
 * <p>Typical usage:
 * <pre>{@code
 * AIApi client = new ChatCompletionApiClient(apiKey, apiUrl, AIProvider.OPENAI);
 * String response = client.execCompletions("Hello!", "You are a helpful assistant.", model);
 * }</pre>
 * <p>
 * This class is reusable and thread-safe.
 */
public class ChatCompletionApiClient implements AIApi {

    private static final Logger logger = LogManager.getLogger(ChatCompletionApiClient.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private final String apiKey;
    private final String apiUrl;
    private final AIProvider aiProvider;

    public ChatCompletionApiClient(String apiKey, String apiUrl, AIProvider aiProvider) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.aiProvider = aiProvider;
    }

    @Override
    public String execCompletions(String userPrompt, String systemPrompt, AIModel model) {
        logger.info("userPrompt: {}", userPrompt);
        logger.info("systemPrompt: {}", systemPrompt);
        logger.info("aiProvider: {}", aiProvider.name());
        logger.info("model: {}", model.getModelName());

        List<Map<String, Object>> messages = AiApiUtils.createMessages(userPrompt, systemPrompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model.getModelName());
        requestBody.put("messages", messages);

        String jsonRequestBody;
        try {
            jsonRequestBody = mapper.writeValueAsString(requestBody);
        } catch (Exception e) {
            throw new RuntimeException("Error creating JSON request body", e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequestBody))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("HTTP request failed", e);
        }

        return AiApiUtils.handleResponse(response, aiProvider());
    }

    @Override
    public AIProvider aiProvider() {
        return aiProvider;
    }
}