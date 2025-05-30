package com.avpuser.ai.executor;

import java.util.Optional;

/**
 * Interface for caching AI prompt responses.
 */
public interface PromptCacheService {

    /**
     * Attempts to retrieve a cached response for the given prompt request.
     *
     * @param request The AI prompt request.
     * @return An optional containing the cached response (already deserialized) if found.
     */
    Optional<String> findCached(AiPromptRequest request);

    /**
     * Saves the response for the given prompt request to the cache.
     *
     * @param request  The AI prompt request.
     * @param response The response to be cached.
     */
    void save(AiPromptRequest request, String response);
}