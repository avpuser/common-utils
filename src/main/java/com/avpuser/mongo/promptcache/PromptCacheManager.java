package com.avpuser.mongo.promptcache;

import com.avpuser.gpt.executor.StringPromptRequest;
import com.avpuser.mongo.CommonDao;
import com.avpuser.mongo.CommonManager;
import com.avpuser.mongo.DbEntity;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Map;
import java.util.Optional;

public class PromptCacheManager extends CommonManager<PromptCache> {

    public PromptCacheManager(Map<Class<?>, CommonDao<? extends DbEntity>> allDaos) {
        super(allDaos, PromptCache.class);
    }

    public Optional<String> findCached(StringPromptRequest request) {
        String id = buildRequestHashKey(request);
        return findById(id).map(PromptCache::getResponse);
    }

    public void save(StringPromptRequest request, String response) {
        String id = buildRequestHashKey(request);
        insert(new PromptCache(id, request, response));
    }

    private String buildRequestHashKey(StringPromptRequest request) {
        String base = String.join("::",
                request.getPromptType(),
                request.getModel().getModelName(),
                request.getRequest()
        );
        return DigestUtils.sha256Hex(base);
    }
}
