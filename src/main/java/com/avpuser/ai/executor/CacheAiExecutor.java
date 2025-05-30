package com.avpuser.ai.executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CacheAiExecutor implements AiExecutor {

    private static final Logger logger = LogManager.getLogger(CacheAiExecutor.class);

    private final AiExecutor aiExecutor;
    private final PromptCacheService promptCacheService;

    public CacheAiExecutor(
            AiExecutor aiExecutor,
            PromptCacheService promptCacheService
    ) {
        this.aiExecutor = aiExecutor;
        this.promptCacheService = promptCacheService;
    }

    @Override
    public <TRequest, TResponse> TResponse executeAndExtractContent(TypedPromptRequest<TRequest, TResponse> request) {
        return promptCacheService.findCached(request)
                .map(cached -> {
                    logger.info("Cache hit for: {}", request.getPromptType());
                    return cached;
                })
                .orElseGet(() -> {
                    logger.info("Cache miss for: {}", request.getPromptType());

                    TResponse response = aiExecutor.executeAndExtractContent(request);
                    promptCacheService.save(request, response);
                    return response;
                });
    }
}