package com.avpuser.ai;

/**
 * Represents a generic interface for interacting with AI chat completion APIs.
 * <p>
 * Implementations of this interface wrap different AI providers (e.g., OpenAI, DeepSeek, Claude)
 * and standardize how prompts are sent and responses are received.
 */
public interface AIApi {
    /**
     * Executes a chat completion request using the given user and system prompts and model.
     *
     * @param userPrompt   the message from the user (e.g., a question or instruction)
     * @param systemPrompt the instruction to guide the model's behavior (e.g., role definition)
     * @param model        the model to be used for the request (e.g., gpt-4, deepseek-chat)
     * @return the raw response from the AI provider as a String
     */
    String execCompletions(String userPrompt, String systemPrompt, AIModel model);

    /**
     * Returns the {@link AIProvider} enum representing the underlying AI provider.
     *
     * @return the provider (e.g., OPENAI, DEEPSEEK)
     */
    AIProvider aiProvider();

    /** Возвращает true, если API уже возвращает финальный текст, а не JSON. */
    default boolean returnsPlainText() {
        return false;
    }
}
