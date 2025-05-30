package com.avpuser.gpt.executor;

import com.avpuser.utils.JsonUtils;
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
        boolean isRequestString = request.getRequest() instanceof String;
        boolean isResponseString = request.getResponseClass().equals(String.class);

        String promptPayload = isRequestString
                ? (String) request.getRequest()
                : JsonUtils.toJson(request.getRequest());

        StringPromptRequest promptRequest = new StringPromptRequest(
                promptPayload,
                request.getSystemContext(),
                request.getModel(),
                request.getProgressListener(),
                request.getPromptType()
        );

        return promptCacheService.findCached(promptRequest)
                .map(cached -> {
                    logger.info("Cache hit for: {}", request.getPromptType());
                    if (isResponseString) {
                        return (TResponse) cached;
                    } else {
                        return JsonUtils.deserializeJsonToObject(cached, request.getResponseClass());
                    }
                })
                .orElseGet(() -> {
                    logger.info("Cache miss for: {}", request.getPromptType());

                    TResponse response = aiExecutor.executeAndExtractContent(request);

                    String serializedResponse = (response instanceof String)
                            ? (String) response
                            : JsonUtils.toJson(response);

                    promptCacheService.save(promptRequest, serializedResponse);
                    return response;
                });
    }
}