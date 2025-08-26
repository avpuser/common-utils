package ai;

import com.avpuser.ai.AIModel;
import com.avpuser.ai.executor.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CacheAiExecutorTest {

    private AiExecutor mockAiExecutor;
    private PromptCacheService mockCache;
    private CacheAiExecutor cacheExecutor;
    private AiPromptRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockAiExecutor = mock(AiExecutor.class);
        mockCache = mock(PromptCacheService.class);
        cacheExecutor = new CacheAiExecutor(mockAiExecutor, mockCache);
        mockRequest = mock(AiPromptRequest.class);
    }

    @Test
    void shouldReturnCachedValue_WhenCacheHit() {
        when(mockRequest.getPromptType()).thenReturn("test_prompt");
        when(mockCache.findCached(mockRequest)).thenReturn(Optional.of(new AiResponse("cached-response", AIModel.GPT_4O)));

        AiResponse result = cacheExecutor.execute(mockRequest);

        assertEquals("cached-response", result.getResponse());
        verify(mockCache).findCached(mockRequest);
        verify(mockAiExecutor, never()).execute(any());
        verify(mockCache, never()).save(any(), any());
    }

    @Test
    void shouldCallAiExecutorAndSave_WhenCacheMiss() {
        when(mockRequest.getPromptType()).thenReturn("missing_prompt");
        when(mockCache.findCached(mockRequest)).thenReturn(Optional.empty());
        when(mockAiExecutor.execute(mockRequest)).thenReturn(new AiResponse("fresh-response", AIModel.GPT_4O));

        AiResponse result = cacheExecutor.execute(mockRequest);

        assertEquals("fresh-response", result.getResponse());
        verify(mockCache).findCached(mockRequest);
        verify(mockAiExecutor).execute(mockRequest);
        verify(mockCache).save(mockRequest, new AiResponse("fresh-response", AIModel.GPT_4O));
    }

    @Test
    void shouldThrowException_WhenSaveFails() {
        when(mockRequest.getPromptType()).thenReturn("error_on_save");
        when(mockCache.findCached(mockRequest)).thenReturn(Optional.empty());
        when(mockAiExecutor.execute(mockRequest)).thenReturn(new AiResponse("response", AIModel.GPT_4O));

        doThrow(new RuntimeException("save failed"))
                .when(mockCache).save(mockRequest, new AiResponse("response", AIModel.GPT_4O));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> cacheExecutor.execute(mockRequest));
        assertEquals("save failed", ex.getMessage());

        verify(mockCache).findCached(mockRequest);
        verify(mockAiExecutor).execute(mockRequest);
        verify(mockCache).save(mockRequest, new AiResponse("response", AIModel.GPT_4O));
    }

}