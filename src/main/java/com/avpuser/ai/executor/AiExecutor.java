package com.avpuser.ai.executor;

/**
 * Core interface for executing typed AI requests using a specified model and configuration.
 * <p>
 * Implementations of this interface handle the transformation of {@link AiPromptRequest}
 * into actual calls to an AI provider, managing input/output serialization as needed.
 * <p>
 * Behavior may vary depending on the type of input and expected output:
 * <ul>
 *     <li>If both input and output are {@code String}, the request is treated as a raw prompt.</li>
 *     <li>If structured objects are used, implementations may serialize input to JSON and
 *         deserialize the response accordingly.</li>
 * </ul>
 *
 * <p>This abstraction allows pluggable strategies such as caching, progress tracking,
 * retry logic, or custom logging to be layered transparently over the base executor.</p>
 *
 * @see AiPromptRequest
 * @see PromptCacheService
 */
public interface AiExecutor {

    /**
     * Executes a typed AI request.
     *
     * @param request An {@link AiPromptRequest} containing user/system prompts,
     *                model information, prompt type, and optional progress listener.
     * @return A response string returned by the AI model, either raw or deserialized.
     */
    AiResponse execute(AiPromptRequest request);
}