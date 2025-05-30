package ai;


import com.avpuser.ai.AIModel;
import com.avpuser.ai.executor.AiExecutor;
import com.avpuser.ai.executor.CacheAiExecutor;
import com.avpuser.ai.executor.PromptCacheService;
import com.avpuser.ai.executor.TypedPromptRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class CacheAiExecutorTest {

    private AiExecutor mockExecutor;
    private PromptCacheService mockCache;
    private CacheAiExecutor cacheAiExecutor;

    @BeforeEach
    public void setup() {
        mockExecutor = mock(AiExecutor.class);
        mockCache = mock(PromptCacheService.class);
        cacheAiExecutor = new CacheAiExecutor(mockExecutor, mockCache);
    }

    @Test
    public void testExecuteAndExtractContent_cacheHit() {
        TypedPromptRequest<String, String> request = TypedPromptRequest.of(
                "prompt", "sys", String.class, AIModel.DEEPSEEK_CHAT, "test"
        );

        when(mockCache.findCached(request)).thenReturn(Optional.of("cached-response"));

        String result = cacheAiExecutor.executeAndExtractContent(request);

        assertEquals("cached-response", result);

        verify(mockCache, times(1)).findCached(request);
        verifyNoMoreInteractions(mockExecutor);
    }

    @Test
    public void testExecuteAndExtractContent_cacheMiss() {
        TypedPromptRequest<String, String> request = TypedPromptRequest.of(
                "prompt", "sys", String.class, AIModel.DEEPSEEK_CHAT, "test"
        );

        when(mockCache.findCached(request)).thenReturn(Optional.empty());
        when(mockExecutor.executeAndExtractContent(request)).thenReturn("real-response");

        String result = cacheAiExecutor.executeAndExtractContent(request);

        assertEquals("real-response", result);

        verify(mockCache, times(1)).findCached(request);
        verify(mockExecutor, times(1)).executeAndExtractContent(request);
        verify(mockCache, times(1)).save(request, "real-response");
    }
}