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
    <TRequest, TResponse> Optional<TResponse> findCached(TypedPromptRequest<TRequest, TResponse> request);

    /**
     * Saves the response for the given prompt request to the cache.
     *
     * @param request  The AI prompt request.
     * @param response The response to be cached.
     */
    <TRequest, TResponse> void save(TypedPromptRequest<TRequest, TResponse> request, TResponse response);
}