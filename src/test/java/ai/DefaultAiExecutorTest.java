package ai;

import com.avpuser.ai.AIModel;
import com.avpuser.ai.deepseek.DeepSeekApi;
import com.avpuser.ai.executor.AiPromptRequest;
import com.avpuser.ai.executor.DefaultAiExecutor;
import com.avpuser.ai.openai.OpenAIApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultAiExecutorTest {

    private OpenAIApi openAiApi;
    private DeepSeekApi deepSeekApi;
    private DefaultAiExecutor executor;

    @BeforeEach
    void setUp() {
        openAiApi = mock(OpenAIApi.class);
        deepSeekApi = mock(DeepSeekApi.class);
        executor = new DefaultAiExecutor(openAiApi, deepSeekApi);
    }

    @Test
    void shouldExecuteUsingOpenAI() {
        AiPromptRequest request = AiPromptRequest.of("hello", "system", AIModel.GPT_4, "test");

        when(openAiApi.execCompletions("hello", "system", AIModel.GPT_4))
                .thenReturn("{\"choices\":[{\"message\":{\"content\":\"Hi!\"}}]}");

        String result = executor.execute(request);

        assertEquals("Hi!", result);
        verify(openAiApi).execCompletions("hello", "system", AIModel.GPT_4);
        verifyNoInteractions(deepSeekApi);
    }

    @Test
    void shouldExecuteUsingDeepSeek() {
        AiPromptRequest request = AiPromptRequest.of("ping", "system", AIModel.DEEPSEEK_CHAT, "test");

        when(deepSeekApi.execCompletions("ping", "system", AIModel.DEEPSEEK_CHAT))
                .thenReturn("{\"choices\":[{\"message\":{\"content\":\"pong\"}}]}");

        String result = executor.execute(request);

        assertEquals("pong", result);
        verify(deepSeekApi).execCompletions("ping", "system", AIModel.DEEPSEEK_CHAT);
        verifyNoInteractions(openAiApi);
    }

    @Test
    void shouldThrowOnBlankUserPrompt() {
        AiPromptRequest request = AiPromptRequest.of("  ", "context", AIModel.GPT_4, "test");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> executor.execute(request));
        assertEquals("userInput", ex.getMessage());
    }

    @Test
    void shouldThrowOnBlankSystemPrompt() {
        AiPromptRequest request = AiPromptRequest.of("question", "   ", AIModel.GPT_4, "test");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> executor.execute(request));
        assertEquals("systemContext", ex.getMessage());
    }

}