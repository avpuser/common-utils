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