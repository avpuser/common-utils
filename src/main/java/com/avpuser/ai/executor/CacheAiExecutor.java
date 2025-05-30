package com.avpuser.ai.executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Decorator for {@link AiExecutor} that adds caching support to AI prompt execution.
 * <p>
 * This executor checks if a response for a given {@link AiPromptRequest} already exists
 * in the {@link PromptCacheService}. If a cached response is found, it is returned immediately
 * without invoking the underlying AI model. Otherwise, the request is executed via the
 * delegated {@link AiExecutor}, and the result is cached for future reuse.
 * </p>
 *
 * <p><strong>Use case:</strong> Ideal for expensive or idempotent AI requests
 * where duplicate prompts should reuse existing results to reduce latency and cost.</p>
 *
 * <p><strong>Behavior:</strong></p>
 * <ul>
 *     <li>Checks the cache for a matching prompt response using {@link PromptCacheService#findCached}.</li>
 *     <li>Returns the cached response if found.</li>
 *     <li>If not found, executes the prompt via the underlying {@link AiExecutor},
 *         then stores the result in the cache using {@link PromptCacheService#save}.</li>
 *     <li>Logs whether a cache hit or miss occurred.</li>
 * </ul>
 *
 * <p><strong>Thread safety:</strong> This class is thread-safe only if the underlying
 * {@code PromptCacheService} and {@code AiExecutor} are thread-safe.</p>
 *
 * <p><strong>Example:</strong></p>
 * <pre>{@code
 * AiExecutor rawExecutor = new OpenAiExecutor(...);
 * PromptCacheService cacheService = new PromptCacheManager(...);
 * AiExecutor cachedExecutor = new CacheAiExecutor(rawExecutor, cacheService);
 *
 * AiPromptRequest request = AiPromptRequest.of("Prompt", "Context", AIModel.GPT_4, "example");
 * String response = cachedExecutor.execute(request);
 * }</pre>
 *
 * @see AiExecutor
 * @see PromptCacheService
 * @see AiPromptRequest
 */
public class CacheAiExecutor implements AiExecutor {

    private static final Logger logger = LogManager.getLogger(CacheAiExecutor.class);

    private final AiExecutor aiExecutor;
    private final PromptCacheService promptCacheService;

    /**
     * Constructs a new {@code CacheAiExecutor} that wraps an existing executor with caching capabilities.
     *
     * @param aiExecutor         the delegate executor used when cache misses occur
     * @param promptCacheService the service responsible for caching prompt results
     */
    public CacheAiExecutor(
            AiExecutor aiExecutor,
            PromptCacheService promptCacheService
    ) {
        this.aiExecutor = aiExecutor;
        this.promptCacheService = promptCacheService;
    }

    /**
     * Executes the given prompt request, using cached response if available.
     *
     * @param request the typed AI prompt request
     * @return the AI model's response string, either from cache or from live execution
     */
    @Override
    public String execute(AiPromptRequest request) {
        return promptCacheService.findCached(request)
                .map(cached -> {
                    logger.debug("Cache hit for: {}", request.getPromptType());
                    return cached;
                })
                .orElseGet(() -> {
                    logger.debug("Cache miss for: {}", request.getPromptType());

                    String response = aiExecutor.execute(request);
                    promptCacheService.save(request, response);
                    return response;
                });
    }
}