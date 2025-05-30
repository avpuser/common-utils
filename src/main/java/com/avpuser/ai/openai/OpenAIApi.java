package com.avpuser.ai.openai;

import com.avpuser.ai.AIApi;
import com.avpuser.ai.AIModel;
import com.avpuser.ai.AIProvider;
import com.avpuser.ai.AiResponseParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenAIApi implements AIApi {

    private final static Logger logger = LogManager.getLogger(OpenAIApi.class);

    private final String apiKey;

    private final HttpClient client;

    public OpenAIApi(String apiKey) {
        this.apiKey = apiKey;
        this.client = HttpClient.newHttpClient();
    }

    @Override
    public String execCompletions(String userPrompt, String systemPrompt, AIModel model) {
        logger.info("userPrompt: " + userPrompt);
        logger.info("systemPrompt: " + systemPrompt);
        logger.info("model: " + model.getModelName());

        List<Map<String, Object>> messages = createMessages(userPrompt, systemPrompt);

        // Building the request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model.getModelName());
        requestBody.put("messages", messages);

        // Serializing the body to JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonRequestBody;
        try {
            jsonRequestBody = objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error while creating JSON request body", e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequestBody))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error during HTTP-request", e);
        }

        String body = response.body();
        if (response.statusCode() != 200) {
            logger.error("Error API OpenAI. Code: " + response.statusCode() + ", response: " + body);
            throw new RuntimeException("Error API OpenAI: " + response.statusCode());
        }

        if (AiResponseParser.isResponseCutOff(body)) {
            logger.error("Response from ai is cut off.");
        }

        logger.info("Response body: " + body);
        return body;
    }

    @Override
    public AIProvider aiProvider() {
        return AIProvider.OPENAI;
    }

    public List<Map<String, Object>> createMessages(String userPrompt, String systemPrompt) {
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
}
