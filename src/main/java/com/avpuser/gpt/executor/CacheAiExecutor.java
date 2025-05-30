package com.avpuser.gpt.executor;

import com.avpuser.mongo.promptcache.PromptCacheManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CacheAiExecutor implements AiExecutor {

    private static final Logger logger = LogManager.getLogger(CacheAiExecutor.class);

    private final AiExecutor aiExecutor;
    private final PromptCacheService promptCacheService;

    public CacheAiExecutor(
            AiExecutor aiExecutor,
            PromptCacheManager promptCacheService
    ) {
        this.aiExecutor = aiExecutor;
        this.promptCacheService = promptCacheService;
    }

    @Override
    public <TRequest, TResponse> TResponse executeAndExtractContent(TypedPromptRequest<TRequest, TResponse> request) {
        if (request.getRequest() instanceof String && request.getResponseClass() == String.class) {
            StringPromptRequest promptRequest = new StringPromptRequest(
                    (String) request.getRequest(),
                    request.getSystemContext(),
                    request.getModel(),
                    request.getProgressListener(),
                    request.getPromptType()
            );

            return promptCacheService.findCached(promptRequest)
                    .map(cached -> {
                        logger.info("Cache hit for: {}", request.getPromptType());
                        return (TResponse) cached;
                    })
                    .orElseGet(() -> {
                        logger.info("Cache miss for: {}", request.getPromptType());
                        String response = aiExecutor.executeAndExtractContent(promptRequest);
                        promptCacheService.save(promptRequest, response);
                        return (TResponse) response;
                    });
        }

        throw new UnsupportedOperationException(
                "Cache is only supported for TypedPromptRequest<String, String>, but got: " +
                        request.getRequest().getClass().getSimpleName() + " -> " +
                        request.getResponseClass().getSimpleName()
        );
    }
}
