package ai;

import com.avpuser.ai.AIModel;
import com.avpuser.ai.executor.AiPromptRequest;
import com.avpuser.mongo.CommonDao;
import com.avpuser.mongo.DbEntity;
import com.avpuser.mongo.promptcache.PromptCache;
import com.avpuser.mongo.promptcache.PromptCacheKeyUtils;
import com.avpuser.mongo.promptcache.PromptCacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PromptCacheManagerTest {

    private CommonDao<PromptCache> promptCacheDao;
    private PromptCacheManager manager;

    private static final String USER_PROMPT = "Hello";
    private static final String SYSTEM_PROMPT = "System";
    private static final String PROMPT_TYPE = "test-type";
    private static final String RESPONSE = "AI response";
    private static final AIModel MODEL = AIModel.GPT_4;

    @BeforeEach
    void setUp() {
        //noinspection unchecked
        promptCacheDao = mock(CommonDao.class);

        Map<Class<?>, CommonDao<? extends DbEntity>> daoMap = new HashMap<>();
        daoMap.put(PromptCache.class, promptCacheDao);

        manager = new PromptCacheManager(daoMap);
    }

    @Test
    void findCached_whenExists_shouldReturnResponse() {
        AiPromptRequest request = AiPromptRequest.of(USER_PROMPT, SYSTEM_PROMPT, MODEL, PROMPT_TYPE);
        String hashKey = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, USER_PROMPT, SYSTEM_PROMPT, MODEL);

        PromptCache cached = new PromptCache(hashKey, USER_PROMPT, SYSTEM_PROMPT, RESPONSE, PROMPT_TYPE, MODEL);
        when(promptCacheDao.findById(hashKey)).thenReturn(Optional.of(cached));

        Optional<String> result = manager.findCached(request);
        assertTrue(result.isPresent());
        assertEquals(RESPONSE, result.get());
    }

    @Test
    void findCached_whenNotExists_shouldReturnEmpty() {
        AiPromptRequest request = AiPromptRequest.of(USER_PROMPT, SYSTEM_PROMPT, MODEL, PROMPT_TYPE);
        String hashKey = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, USER_PROMPT, SYSTEM_PROMPT, MODEL);

        when(promptCacheDao.findById(hashKey)).thenReturn(Optional.empty());

        Optional<String> result = manager.findCached(request);
        assertTrue(result.isEmpty());
    }

    @Test
    void save_whenNotExists_shouldInsert() {
        AiPromptRequest request = AiPromptRequest.of(USER_PROMPT, SYSTEM_PROMPT, MODEL, PROMPT_TYPE);
        String hashKey = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, USER_PROMPT, SYSTEM_PROMPT, MODEL);

        when(promptCacheDao.findById(hashKey)).thenReturn(Optional.empty());

        manager.save(request, RESPONSE);

        verify(promptCacheDao).insert(argThat(cache ->
                cache.getId().equals(hashKey) &&
                        cache.getResponse().equals(RESPONSE) &&
                        cache.getUserPrompt().equals(USER_PROMPT) &&
                        cache.getSystemPrompt().equals(SYSTEM_PROMPT)
        ));
    }

    @Test
    void save_whenExists_shouldUpdate() {
        AiPromptRequest request = AiPromptRequest.of(USER_PROMPT, SYSTEM_PROMPT, MODEL, PROMPT_TYPE);
        String hashKey = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, USER_PROMPT, SYSTEM_PROMPT, MODEL);

        PromptCache existing = new PromptCache(hashKey, USER_PROMPT, SYSTEM_PROMPT, "old", PROMPT_TYPE, MODEL);
        when(promptCacheDao.findById(hashKey)).thenReturn(Optional.of(existing));

        manager.save(request, RESPONSE);

        verify(promptCacheDao).update(argThat(cache ->
                cache.getId().equals(hashKey) &&
                        cache.getResponse().equals(RESPONSE)
        ));
    }
}