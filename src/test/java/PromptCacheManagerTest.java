import com.avpuser.gpt.AIModel;
import com.avpuser.gpt.executor.StringPromptRequest;
import com.avpuser.mongo.CommonDao;
import com.avpuser.mongo.promptcache.PromptCache;
import com.avpuser.mongo.promptcache.PromptCacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        StringPromptRequest request = new StringPromptRequest("hello", "ctx", AIModel.GPT_4, null, "type1");
        String expectedResponse = "cached result";

        String id = computeId(request);
        when(dao.findById(id)).thenReturn(Optional.of(new PromptCache(id, request, expectedResponse)));

        Optional<String> result = manager.findCached(request);

        assertTrue(result.isPresent());
        assertEquals(expectedResponse, result.get());
        verify(dao).findById(id);
    }

    @Test
    void testFindCached_Miss() {
        StringPromptRequest request = new StringPromptRequest("hello", "ctx", AIModel.GPT_4, null, "type1");

        String id = computeId(request);
        when(dao.findById(id)).thenReturn(Optional.empty());

        Optional<String> result = manager.findCached(request);

        assertTrue(result.isEmpty());
        verify(dao).findById(id);
    }

    @Test
    void testSave_InsertsPromptCache() {
        StringPromptRequest request = new StringPromptRequest("input", "ctx", AIModel.GPT_4, null, "typeX");
        String response = "generated result";
        String id = computeId(request);

        manager.save(request, response);

        verify(dao).insert(argThat(pc ->
                pc.getId().equals(id) &&
                        pc.getResponse().equals(response) &&
                        pc.getRequest().getPromptType().equals("typeX")
        ));
    }

    private String computeId(StringPromptRequest request) {
        // This should match buildRequestHashKey in PromptCacheManager
        String base = String.join("::",
                request.getPromptType(),
                request.getModel().getModelName(),
                request.getRequest()
        );
        return org.apache.commons.codec.digest.DigestUtils.sha256Hex(base);
    }
}