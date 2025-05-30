import com.avpuser.ai.AIModel;
import com.avpuser.ai.executor.StringPromptRequest;
import com.avpuser.mongo.CommonDao;
import com.avpuser.mongo.promptcache.PromptCache;
import com.avpuser.mongo.promptcache.PromptCacheKeyUtils;
import com.avpuser.mongo.promptcache.PromptCacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PromptCacheManagerTest {

    private CommonDao<PromptCache> dao;
    private PromptCacheManager manager;

    @BeforeEach
    void setup() {
        dao = mock(CommonDao.class);
        manager = new PromptCacheManager(Map.of(PromptCache.class, dao));
    }

    @Test
    void testFindCached_Hit() {
        StringPromptRequest request = new StringPromptRequest("hello", "sys", AIModel.GPT_4, null, "test_type");
        String expectedResponse = "cached result";

        String id = PromptCacheKeyUtils.buildHashKey(request);
        PromptCache cached = new PromptCache(id, request.getRequest(), expectedResponse, request.getPromptType(), request.getModel());

        when(dao.findById(id)).thenReturn(Optional.of(cached));

        Optional<String> result = manager.findCached(request);

        assertTrue(result.isPresent());
        assertEquals(expectedResponse, result.get());
        verify(dao).findById(id);
    }

    @Test
    void testFindCached_Miss() {
        StringPromptRequest request = new StringPromptRequest("missed", "sys", AIModel.GPT_4, null, "not_found_type");

        String id = PromptCacheKeyUtils.buildHashKey(request);
        when(dao.findById(id)).thenReturn(Optional.empty());

        Optional<String> result = manager.findCached(request);

        assertTrue(result.isEmpty());
        verify(dao).findById(id);
    }

    @Test
    void testSave_InsertsPromptCache() {
        StringPromptRequest request = new StringPromptRequest("save this", "sys", AIModel.DEEPSEEK_CHAT, null, "save_type");
        String response = "generated";

        String id = PromptCacheKeyUtils.buildHashKey(request);

        manager.save(request, response);

        verify(dao).insert(argThat(pc ->
                pc.getId().equals(id) &&
                        pc.getRequest().equals(request.getRequest()) &&
                        pc.getResponse().equals(response) &&
                        pc.getPromptType().equals(request.getPromptType()) &&
                        pc.getModel() == request.getModel()
        ));
    }
}