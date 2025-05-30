package com.avpuser.ai.executor;

import java.util.Optional;

/**
 * Interface for a cache service that stores and retrieves AI prompt responses.
 * <p>
 * Implementations of this interface are responsible for identifying a unique key
 * for each {@link AiPromptRequest} and associating it with a corresponding response.
 * </p>
 *
 * <p>Use cases include:</p>
 * <ul>
 *   <li>Reducing redundant AI calls for repeated inputs</li>
 *   <li>Improving performance and lowering cost by avoiding duplicate computation</li>
 *   <li>Providing a fast-access layer for frequently used prompts</li>
 * </ul>
 *
 * @see AiPromptRequest
 */
public interface PromptCacheService {

    /**
     * Attempts to retrieve a previously cached AI response for the given prompt request.
     *
     * @param request The AI prompt request used as the cache key.
     * @return An {@link Optional} containing the cached response (as a raw string), if found.
     *         If no matching entry exists, returns {@link Optional#empty()}.
     */
    Optional<String> findCached(AiPromptRequest request);

    /**
     * Stores the given response in the cache, using the request as a key.
     * If a response already exists for the same key, it may be updated or replaced.
     *
     * @param request  The original prompt request that was executed.
     * @param response The resulting response to be cached.
     */
    void save(AiPromptRequest request, String response);
}