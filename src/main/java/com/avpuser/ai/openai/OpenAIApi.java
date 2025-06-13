package com.avpuser.ai.openai;

import com.avpuser.ai.AIApi;
import com.avpuser.ai.AIModel;
import com.avpuser.ai.AIProvider;
import com.avpuser.ai.ChatCompletionApiClient;

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

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    private static final AIProvider AI_PROVIDER = AIProvider.OPENAI;

    private final ChatCompletionApiClient chatCompletionApiClient;

    public OpenAIApi(String apiKey) {
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