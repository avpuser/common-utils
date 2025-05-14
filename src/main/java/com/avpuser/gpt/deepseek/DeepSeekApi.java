package com.avpuser.gpt.deepseek;

import com.avpuser.gpt.GptResponseParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class DeepSeekApi {

    private final static Logger logger = LogManager.getLogger(DeepSeekApi.class);
    private final String apiKey;
    private final HttpClient client;

    public DeepSeekApi(String apiKey) {
        this.apiKey = apiKey;
        this.client = HttpClient.newHttpClient();
    }

    public String execCompletions(String userInput, String systemContext, DeepSeekModel model) {
        logger.info("userInput: " + userInput);
        logger.info("systemContext: " + systemContext);
        logger.info("model: " + model.getModelName());

        List<Map<String, Object>> messages = createMessages(userInput, systemContext);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model.getModelName());
        requestBody.put("messages", messages);

        String jsonRequestBody;
        try {
            jsonRequestBody = new ObjectMapper().writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка при создании JSON тела запроса", e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.deepseek.com/v1/chat/completions"))
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
            logger.error("Error API DeepSeek. Code: " + response.statusCode() + ", response: " + body);
            throw new RuntimeException("Error API DeepSeek: " + response.statusCode());
        }

        logger.info("Response body: " + body);
        if (GptResponseParser.isResponseCutOff(body)) {
            logger.error("Response from AI is cut off.");
        }

        return body;
    }

    private List<Map<String, Object>> createMessages(String userInput, String systemContext) {
        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemContext);
        messages.add(systemMessage);

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userInput);
        messages.add(userMessage);

        return messages;
    }
}