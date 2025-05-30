package ai;

import com.avpuser.ai.AIModel;
import com.avpuser.ai.executor.TypedPromptRequest;
import com.avpuser.mongo.CommonDao;
import com.avpuser.mongo.DbEntity;
import com.avpuser.mongo.promptcache.PromptCache;
import com.avpuser.mongo.promptcache.PromptCacheKeyUtils;
import com.avpuser.mongo.promptcache.PromptCacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PromptCacheManagerTest {

    private CommonDao<PromptCache> mockDao;
    private PromptCacheManager manager;

    @BeforeEach
    public void setup() {
        mockDao = mock(CommonDao.class);
        Map<Class<?>, CommonDao<? extends DbEntity>> allDaos = Map.of(PromptCache.class, mockDao);
        manager = new PromptCacheManager(allDaos);
    }

    @Test
    public void testFindCached_hit() {
        String requestPayload = "{\"prompt\":\"prompt\"}";
        String expectedResponse = "{\"loinc_code\":\"26515-7\"}";
        String promptType = "loinc_code_resolve";
        AIModel model = AIModel.DEEPSEEK_CHAT;
        String id = PromptCacheKeyUtils.buildHashKey(promptType, requestPayload, model);

        PromptCache mockEntity = new PromptCache(id, requestPayload, expectedResponse, promptType, model);

        when(mockDao.findById(id)).thenReturn(Optional.of(mockEntity));

        TypedPromptRequest<String, String> request = TypedPromptRequest.of(
                requestPayload,
                "system",
                String.class,
                model,
                promptType
        );

        Optional<String> result = manager.findCached(request);
        assertTrue(result.isPresent());
        assertEquals(expectedResponse, result.get());
    }

    @Test
    public void testFindCached_miss() {
        String requestPayload = "no-hit";
        String promptType = "test";
        AIModel model = AIModel.DEEPSEEK_CHAT;
        String id = PromptCacheKeyUtils.buildHashKey(promptType, requestPayload, model);

        when(mockDao.findById(id)).thenReturn(Optional.empty());

        TypedPromptRequest<String, String> request = TypedPromptRequest.of(
                requestPayload,
                "sys",
                String.class,
                model,
                promptType
        );

        Optional<String> result = manager.findCached(request);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSave_insert() {
        String requestPayload = "insert-payload";
        String response = "{\"result\":\"ok\"}";
        String promptType = "insert_test";
        AIModel model = AIModel.DEEPSEEK_CHAT;
        String id = PromptCacheKeyUtils.buildHashKey(promptType, requestPayload, model);

        when(mockDao.findById(id)).thenReturn(Optional.empty());

        TypedPromptRequest<String, String> request = TypedPromptRequest.of(
                requestPayload,
                "ctx",
                String.class,
                model,
                promptType
        );

        manager.save(request, response);

        verify(mockDao).insert(Mockito.argThat(saved ->
                saved.getId().equals(id)
                        && saved.getRequest().equals(requestPayload)
                        && saved.getResponse().equals(response)
                        && saved.getPromptType().equals(promptType)
                        && saved.getModel().equals(model)
        ));
    }

    @Test
    public void testSave_update() {
        String requestPayload = "existing-payload";
        String response = "{\"status\":\"updated\"}";
        String promptType = "update_test";
        AIModel model = AIModel.DEEPSEEK_CHAT;
        String id = PromptCacheKeyUtils.buildHashKey(promptType, requestPayload, model);

        PromptCache existing = new PromptCache(id, requestPayload, "old", promptType, model);
        when(mockDao.findById(id)).thenReturn(Optional.of(existing));

        TypedPromptRequest<String, String> request = TypedPromptRequest.of(
                requestPayload,
                "ctx",
                String.class,
                model,
                promptType
        );

        manager.save(request, response);

        verify(mockDao).update(Mockito.argThat(updated ->
                updated.getId().equals(id)
                        && updated.getResponse().equals(response)
        ));
    }
}