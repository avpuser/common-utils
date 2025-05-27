package com.avpuser.gpt.openai;

import com.avpuser.gpt.AIModel;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenAIApi {

    private final static Logger logger = LogManager.getLogger(OpenAIApi.class);

    private final String apiKey;

    private final HttpClient client;

    public OpenAIApi(String apiKey) {
        this.apiKey = apiKey;
        this.client = HttpClient.newHttpClient();
    }

    public String execCompletions(String userInput, String systemContext, AIModel model) {
        logger.info("userInput: " + userInput);
        logger.info("systemContext: " + systemContext);
        logger.info("model: " + model.getModelName());

        // Формирование сообщений
        List<Map<String, Object>> messages = createMessages(userInput, systemContext);

        // Формирование тела запроса
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model.getModelName());
        requestBody.put("messages", messages);  // Используем сформированные сообщения

        // Преобразование тела в JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonRequestBody;
        try {
            jsonRequestBody = objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка при создании JSON тела запроса", e);
        }

        // Создание HTTP-запроса
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequestBody))
                .build();

        // Отправка запроса и получение ответа
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

        if (GptResponseParser.isResponseCutOff(body)) {
            logger.error("Response from ai is cut off.");
        }

        logger.info("Response body: " + body);
        return body;
    }

    public List<Map<String, Object>> createMessages(String userInput, String systemContext) {
        // Список сообщений
        List<Map<String, Object>> messages = new ArrayList<>();

        // Сообщение с ролью system
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemContext); // Используем переданный системный контекст
        messages.add(systemMessage);

        // Сообщение с ролью user
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userInput); // Используем введенный текст пользователя
        messages.add(userMessage);

        return messages;
    }
}

