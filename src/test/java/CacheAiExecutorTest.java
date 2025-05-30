import com.avpuser.ai.AIModel;
import com.avpuser.ai.executor.AiExecutor;
import com.avpuser.ai.executor.CacheAiExecutor;
import com.avpuser.ai.executor.PromptCacheService;
import com.avpuser.ai.executor.TypedPromptRequest;
import com.avpuser.utils.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CacheAiExecutorTest {

    private AiExecutor delegateExecutor;
    private PromptCacheService cacheService;
    private CacheAiExecutor cacheAiExecutor;

    @BeforeEach
    void setUp() {
        delegateExecutor = mock(AiExecutor.class);
        cacheService = mock(PromptCacheService.class);
        cacheAiExecutor = new CacheAiExecutor(delegateExecutor, cacheService);
    }

    @Test
    void testCacheHit_StringToString() {
        var request = TypedPromptRequest.of("hello", "ctx", String.class, AIModel.GPT_4, "type1");

        when(cacheService.findCached(any())).thenReturn(Optional.of("cached response"));

        String result = cacheAiExecutor.executeAndExtractContent(request);

        assertEquals("cached response", result);
        verify(cacheService).findCached(any());
        verifyNoInteractions(delegateExecutor); // should not call delegate
    }

    @Test
    void testCacheMiss_StringToString() {
        var request = TypedPromptRequest.of("ask", "ctx", String.class, AIModel.GPT_4, "type2");

        when(cacheService.findCached(any())).thenReturn(Optional.empty());
        when(delegateExecutor.executeAndExtractContent(request)).thenReturn("generated response");

        String result = cacheAiExecutor.executeAndExtractContent(request);

        assertEquals("generated response", result);
        verify(delegateExecutor).executeAndExtractContent(request);
        verify(cacheService).save(any(), eq("generated response"));
    }

    @Test
    void testCacheMiss_ObjectToObject() {
        DummyInput input = new DummyInput("data");
        DummyOutput expected = new DummyOutput("result");

        var request = TypedPromptRequest.of(input, "ctx", DummyOutput.class, AIModel.DEEPSEEK_CHAT, "dummy");

        when(cacheService.findCached(any())).thenReturn(Optional.empty());
        when(delegateExecutor.executeAndExtractContent(request)).thenReturn(expected);

        DummyOutput result = cacheAiExecutor.executeAndExtractContent(request);

        assertEquals("result", result.getResponse());
        verify(delegateExecutor).executeAndExtractContent(request);
        verify(cacheService).save(any(), eq(JsonUtils.toJson(expected)));
    }

    @Test
    void testCacheHit_ObjectToObject() {
        DummyInput input = new DummyInput("x");
        DummyOutput expected = new DummyOutput("cached!");

        var request = TypedPromptRequest.of(input, "ctx", DummyOutput.class, AIModel.DEEPSEEK_CHAT, "dummy");

        String cachedJson = JsonUtils.toJson(expected);
        when(cacheService.findCached(any())).thenReturn(Optional.of(cachedJson));

        DummyOutput result = cacheAiExecutor.executeAndExtractContent(request);

        assertEquals("cached!", result.getResponse());
        verifyNoInteractions(delegateExecutor);
    }

    // Dummy DTOs
    static class DummyInput {
        private String field;

        public DummyInput() {
        }

        public DummyInput(String field) {
            this.field = field;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }
    }

    static class DummyOutput {
        private String response;

        public DummyOutput() {
        }

        public DummyOutput(String response) {
            this.response = response;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }
    }
}