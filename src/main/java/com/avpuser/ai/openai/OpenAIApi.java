package com.avpuser.ai.openai;

import com.avpuser.ai.AIApi;
import com.avpuser.ai.AIModel;
import com.avpuser.ai.AIProvider;
import com.avpuser.ai.AiApiUtils;
import com.avpuser.ai.ChatCompletionApiClient;
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

/**
 * Implementation of {@link AIApi} for communicating with OpenAI's chat completion endpoint.
 * <p>
 * Internally delegates requests to {@link ChatCompletionApiClient}, pre-configured with the OpenAI API URL.
 *
 * <p>Example usage:
 * <pre>{@code
 * AIApi openAiApi = new OpenAIApi("your-api-key");
 * String response = openAiApi.execCompletions("Hello", "You are a helpful assistant.", model);
 * }</pre>
 */
public class OpenAIApi implements AIApi {

    private static final Logger logger = LogManager.getLogger(OpenAIApi.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    private static final AIProvider AI_PROVIDER = AIProvider.OPENAI;

    private final ChatCompletionApiClient chatCompletionApiClient;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final String apiKey;

    public OpenAIApi(String apiKey) {
        this.apiKey = apiKey;
        this.chatCompletionApiClient = new ChatCompletionApiClient(apiKey, API_URL, AI_PROVIDER);
    }

    @Override
    public String execCompletions(String userPrompt, String systemPrompt, AIModel model) {
        return chatCompletionApiClient.execCompletions(userPrompt, systemPrompt, model);
    }

    public String extractTextFromFile(byte[] fileBytes, String mimeType, String prompt) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("File bytes must not be empty");
        }
        if (StringUtils.isBlank(mimeType)) {
            throw new IllegalArgumentException("MIME type must not be blank");
        }
        if (StringUtils.isBlank(prompt)) {
            throw new IllegalArgumentException("Prompt must not be blank");
        }

        AIModel model = AIModel.GPT_4O_MINI;
        logger.info("OpenAI vision exec: model={}, mimeType={}, promptLength={}",
                model.getModelName(), mimeType, prompt.length());

        String requestBody = buildVisionRequest(fileBytes, mimeType, prompt, model);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            AiApiUtils.checkAndThrowIfError(response, aiProvider());
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to call OpenAI vision API", e);
        }
    }

    private String buildVisionRequest(byte[] fileBytes, String mimeType, String prompt, AIModel model) {
        String base64 = Base64.getEncoder().encodeToString(fileBytes);
        String dataUrl = "data:" + mimeType + ";base64," + base64;

        Map<String, Object> body = Map.of(
                "model", model.getModelName(),
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", List.of(
                                        Map.of("type", "text", "text", prompt),
                                        Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))
                                )
                        )
                )
        );

        try {
            return mapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build OpenAI vision request JSON", e);
        }
    }

    @Override
    public AIProvider aiProvider() {
        return AI_PROVIDER;
    }
}