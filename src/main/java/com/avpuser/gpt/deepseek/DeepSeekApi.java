package com.avpuser.gpt.deepseek;

import com.avpuser.progress.EmptyProgressListener;
import com.avpuser.progress.ProgressListener;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DeepSeekApi {

    private final static Logger logger = LogManager.getLogger(DeepSeekApi.class);

    private final String apiKey;

    private final HttpClient client;

    public DeepSeekApi(String apiKey) {
        this.apiKey = apiKey;
        this.client = HttpClient.newHttpClient();
    }

    public String execCompletions(String userInput, String systemContext, DeepSeekModel model) {
        return execCompletions(userInput, systemContext, model, new EmptyProgressListener());
    }

    public String execCompletions(String userInput, String systemContext, DeepSeekModel model, ProgressListener progressListener) {
        logger.info("userInput: " + userInput);
        logger.info("systemContext: " + systemContext);
        logger.info("model: " + model.getModelName());

        List<Map<String, Object>> messages = createMessages(userInput, systemContext);

        progressListener.onProgress(5);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model.getModelName());
        requestBody.put("messages", messages);

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonRequestBody;
        try {
            jsonRequestBody = objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка при создании JSON тела запроса", e);
        }

        progressListener.onProgress(5);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.deepseek.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequestBody))
                .build();

        HttpResponse<String> response;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicInteger progress = new AtomicInteger(5);

        // Поток для обновления прогресса
        Future<?> progressFuture = executor.submit(() -> {
            try {
                while (!completed.get() && progress.get() < 99) {
                    progressListener.onProgress(progress.getAndAdd(1)); // +1%
                    Thread.sleep(1_000); // раз в секунду
                }
            } catch (InterruptedException ignored) {
            }
        });

        // Поток для отправки запроса
        Future<HttpResponse<String>> responseFuture = executor.submit(() -> {
            try {
                return client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Error during HTTP-request", e);
            }
        });

        try {
            response = responseFuture.get(); // блокируемся тут
            completed.set(true);
            progressFuture.cancel(true); // останавливаем прогресс-таймер
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error waiting for completion", e);
        } finally {
            executor.shutdownNow(); // освобождаем потоки
        }


        String body = response.body();
        if (response.statusCode() != 200) {
            logger.error("Error API DeepSeek. Code: " + response.statusCode() + ", response: " + body);
            throw new RuntimeException("Error API DeepSeek: " + response.statusCode());
        }

        logger.info("Response body: " + body);

        progressListener.onProgress(99);
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
