package com.avpuser.ai.deepseek;

import com.avpuser.ai.AIApi;
import com.avpuser.ai.AIModel;
import com.avpuser.ai.AIProvider;
import com.avpuser.ai.ChatCompletionApiClient;

/**
 * Implementation of {@link AIApi} for interacting with the DeepSeek AI chat completion endpoint.
 * <p>
 * Delegates request handling to {@link ChatCompletionApiClient}, pre-configured for DeepSeek.
 *
 * <p>Example usage:
 * <pre>{@code
 * AIApi deepSeekApi = new DeepSeekApi("your-api-key");
 * String response = deepSeekApi.execCompletions("Hi!", "You are a friendly assistant.", model);
 * }</pre>
 */
public class DeepSeekApi implements AIApi {

    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";

    private static final AIProvider AI_PROVIDER = AIProvider.DEEPSEEK;

    private final ChatCompletionApiClient chatCompletionApiClient;

    public DeepSeekApi(String apiKey) {
        this.chatCompletionApiClient = new ChatCompletionApiClient(apiKey, API_URL, AI_PROVIDER);
    }

    @Override
    public String execCompletions(String userPrompt, String systemPrompt, AIModel model) {
        return chatCompletionApiClient.execCompletions(userPrompt, systemPrompt, model);
    }

    @Override
    public AIProvider aiProvider() {
        return AI_PROVIDER;
    }
}
