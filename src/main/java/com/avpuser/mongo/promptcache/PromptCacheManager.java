package com.avpuser.mongo.promptcache;

import com.avpuser.ai.AIModel;
import com.avpuser.ai.executor.AiPromptRequest;
import com.avpuser.ai.executor.PromptCacheService;
import com.avpuser.mongo.CommonDao;
import com.avpuser.mongo.CommonManager;
import com.avpuser.mongo.DbEntity;

import java.util.Map;
import java.util.Optional;

/**
 * Mongo-based implementation of {@link PromptCacheService} that stores and retrieves
 * cached AI prompt responses using a generated hash key.
 *
 * <p>This class extends {@link CommonManager} to leverage generic MongoDB access logic
 * and implements {@link PromptCacheService} to provide a typed API for caching prompts.</p>
 *
 * <p>Each AI request is uniquely identified by a hash key computed from:</p>
 * <ul>
 *   <li>Prompt type (e.g., translation, classification)</li>
 *   <li>User prompt</li>
 *   <li>System prompt</li>
 *   <li>AI model name</li>
 * </ul>
 *
 * <p>Cached entries are stored as {@link PromptCache} documents and either inserted
 * or updated depending on existence.</p>
 *
 * @see PromptCache
 * @see PromptCacheKeyUtils
 */
public class PromptCacheManager extends CommonManager<PromptCache> implements PromptCacheService {

    public PromptCacheManager(Map<Class<?>, CommonDao<? extends DbEntity>> allDaos) {
        super(allDaos, PromptCache.class);
    }

    private Optional<String> internalFindCached(String userPrompt, String systemPrompt, AIModel model, String promptType) {
        String id = PromptCacheKeyUtils.buildHashKey(promptType, userPrompt, systemPrompt, model);
        return findById(id).map(PromptCache::getResponse);
    }

    private void internalSave(String userPrompt, String systemPrompt, AIModel model, String promptType, String response) {
        String id = PromptCacheKeyUtils.buildHashKey(promptType, userPrompt, systemPrompt, model);
        Optional<PromptCache> dbPromptCacheO = findById(id);
        PromptCache promptCache = new PromptCache(id, userPrompt, systemPrompt, response, promptType, model);
        if (dbPromptCacheO.isEmpty()) {
            insert(promptCache);
        } else {
            PromptCache dbPromptCache = dbPromptCacheO.get();
            dbPromptCache.setResponse(response);
            update(dbPromptCache);
        }
    }

    @Override
    public Optional<String> findCached(AiPromptRequest request) {
        return internalFindCached(request.getUserPrompt(), request.getSystemPrompt(), request.getModel(), request.getPromptType());
    }

    @Override
    public void save(AiPromptRequest request, String response) {
        internalSave(request.getUserPrompt(), request.getSystemPrompt(), request.getModel(), request.getPromptType(), response);
    }
}
