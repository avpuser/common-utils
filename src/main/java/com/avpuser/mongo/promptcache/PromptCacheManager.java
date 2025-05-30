package com.avpuser.mongo.promptcache;

import com.avpuser.ai.executor.PromptCacheService;
import com.avpuser.ai.executor.StringPromptRequest;
import com.avpuser.mongo.CommonDao;
import com.avpuser.mongo.CommonManager;
import com.avpuser.mongo.DbEntity;

import java.util.Map;
import java.util.Optional;

public class PromptCacheManager extends CommonManager<PromptCache> implements PromptCacheService {

    public PromptCacheManager(Map<Class<?>, CommonDao<? extends DbEntity>> allDaos) {
        super(allDaos, PromptCache.class);
    }

    public Optional<String> findCached(StringPromptRequest request) {
        String id = PromptCacheKeyUtils.buildHashKey(request);
        return findById(id).map(PromptCache::getResponse);
    }

    public void save(StringPromptRequest request, String response) {
        String id = PromptCacheKeyUtils.buildHashKey(request);
        Optional<PromptCache> dbPromptCacheO = findById(id);
        PromptCache promptCache = new PromptCache(id, request.getRequest(), response, request.getPromptType(), request.getModel());
        if (dbPromptCacheO.isEmpty()) {
            insert(promptCache);
        } else {
            PromptCache dbPromptCache = dbPromptCacheO.get();
            dbPromptCache.setResponse(response);
            update(dbPromptCache);
        }
    }

}
