package com.avpuser.mongo.promptcache;

import com.avpuser.ai.AIModel;
import com.avpuser.ai.executor.PromptCacheService;
import com.avpuser.ai.executor.TypedPromptRequest;
import com.avpuser.mongo.CommonDao;
import com.avpuser.mongo.CommonManager;
import com.avpuser.mongo.DbEntity;
import com.avpuser.utils.JsonUtils;

import java.util.Map;
import java.util.Optional;

public class PromptCacheManager extends CommonManager<PromptCache> implements PromptCacheService {

    public PromptCacheManager(Map<Class<?>, CommonDao<? extends DbEntity>> allDaos) {
        super(allDaos, PromptCache.class);
    }

    private Optional<String> internalFindCached(String request, AIModel model, String promptType) {
        String id = PromptCacheKeyUtils.buildHashKey(promptType, request, model);
        return findById(id).map(PromptCache::getResponse);
    }

    private void internalSave(String request, AIModel model, String promptType, String response) {
        String id = PromptCacheKeyUtils.buildHashKey(promptType, request, model);
        Optional<PromptCache> dbPromptCacheO = findById(id);
        PromptCache promptCache = new PromptCache(id, request, response, promptType, model);
        if (dbPromptCacheO.isEmpty()) {
            insert(promptCache);
        } else {
            PromptCache dbPromptCache = dbPromptCacheO.get();
            dbPromptCache.setResponse(response);
            update(dbPromptCache);
        }
    }

    @Override
    public <TRequest, TResponse> Optional<TResponse> findCached(TypedPromptRequest<TRequest, TResponse> request) {
        String requestPayload = request.getRequest() instanceof String
                ? (String) request.getRequest()
                : JsonUtils.toJson(request.getRequest());

        return internalFindCached(requestPayload, request.getModel(), request.getPromptType()).map(cached -> {
            if (request.getResponseClass().equals(String.class)) {
                return (TResponse) cached;
            } else {
                return JsonUtils.deserializeJsonToObject(cached, request.getResponseClass());
            }
        });
    }

    @Override
    public <TRequest, TResponse> void save(TypedPromptRequest<TRequest, TResponse> request, TResponse tResponse) {
        String requestPayload = request.getRequest() instanceof String
                ? (String) request.getRequest()
                : JsonUtils.toJson(request.getRequest());

        String serializedResponse = tResponse instanceof String
                ? (String) tResponse
                : JsonUtils.toJson(tResponse);

        internalSave(requestPayload, request.getModel(), request.getPromptType(), serializedResponse);
    }
}
