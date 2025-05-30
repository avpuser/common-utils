package ai;

import com.avpuser.ai.AIApi;
import com.avpuser.ai.AIModel;
import com.avpuser.ai.AIProvider;
import com.avpuser.ai.deepseek.DeepSeekApi;
import com.avpuser.ai.executor.AiPromptRequest;
import com.avpuser.ai.executor.DefaultAiExecutor;
import com.avpuser.ai.openai.OpenAIApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DefaultAiExecutorTest {

    private AIApi openAiApi;
    private AIApi deepSeekApi;
    private DefaultAiExecutor executor;

    @BeforeEach
    void setUp() {
        openAiApi = mock(AIApi.class);
        when(openAiApi.aiProvider()).thenReturn(AIProvider.OPENAI);

        deepSeekApi = mock(AIApi.class);
        when(deepSeekApi.aiProvider()).thenReturn(AIProvider.DEEPSEEK);

        executor = new DefaultAiExecutor(List.of(openAiApi, deepSeekApi));
    }

    @Test
    void shouldExecuteUsingOpenAI() {
        OpenAIApi openAiApi = mock(OpenAIApi.class);
        DeepSeekApi deepSeekApi = mock(DeepSeekApi.class);

        when(openAiApi.aiProvider()).thenReturn(AIProvider.OPENAI);
        when(deepSeekApi.aiProvider()).thenReturn(AIProvider.DEEPSEEK);

        DefaultAiExecutor executor = new DefaultAiExecutor(List.of(openAiApi, deepSeekApi));

        AiPromptRequest request = AiPromptRequest.of("hello", "system", AIModel.GPT_4, "test");

        when(openAiApi.execCompletions("hello", "system", AIModel.GPT_4))
                .thenReturn("{\"choices\":[{\"message\":{\"content\":\"Hi!\"}}]}");

        String result = executor.execute(request);

        assertEquals("Hi!", result);
        verify(openAiApi).execCompletions("hello", "system", AIModel.GPT_4);

        // Подтверждаем, что не было доп. вызовов, кроме aiProvider()
        verify(deepSeekApi).aiProvider();
        verifyNoMoreInteractions(deepSeekApi);
    }

    @Test
    void shouldExecuteUsingDeepSeek() {
        OpenAIApi openAiApi = mock(OpenAIApi.class);
        DeepSeekApi deepSeekApi = mock(DeepSeekApi.class);

        when(openAiApi.aiProvider()).thenReturn(AIProvider.OPENAI);
        when(deepSeekApi.aiProvider()).thenReturn(AIProvider.DEEPSEEK);

        DefaultAiExecutor executor = new DefaultAiExecutor(List.of(openAiApi, deepSeekApi));

        AiPromptRequest request = AiPromptRequest.of("ping", "system", AIModel.DEEPSEEK_CHAT, "test");

        when(deepSeekApi.execCompletions("ping", "system", AIModel.DEEPSEEK_CHAT))
                .thenReturn("{\"choices\":[{\"message\":{\"content\":\"pong\"}}]}");

        String result = executor.execute(request);

        assertEquals("pong", result);
        verify(deepSeekApi).execCompletions("ping", "system", AIModel.DEEPSEEK_CHAT);

        // Подтверждаем, что openAiApi использовался только в конструкторе
        verify(openAiApi).aiProvider();
        verifyNoMoreInteractions(openAiApi);
    }

    @Test
    void shouldThrowOnBlankUserPrompt() {
        AiPromptRequest request = AiPromptRequest.of("  ", "context", AIModel.GPT_4, "test");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> executor.execute(request));
        assertEquals("userPrompt must not be blank", ex.getMessage());
    }

    @Test
    void shouldThrowOnBlankSystemPrompt() {
        AiPromptRequest request = AiPromptRequest.of("question", "   ", AIModel.GPT_4, "test");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> executor.execute(request));
        assertEquals("systemPrompt must not be blank", ex.getMessage());
    }

    @Test
    void shouldThrowOnMissingProvider() {
        AIApi openAiApi = mock(AIApi.class);
        when(openAiApi.aiProvider()).thenReturn(AIProvider.OPENAI);

        DefaultAiExecutor executor = new DefaultAiExecutor(List.of(openAiApi));

        // Используем DEEPSEEK_CHAT, которого нет в aiApiMap
        AiPromptRequest request = AiPromptRequest.of("hello", "system", AIModel.DEEPSEEK_CHAT, "test");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> executor.execute(request));
        assertEquals("Unsupported provider: DEEPSEEK", ex.getMessage());
    }

    @Test
    void shouldThrowOnDuplicateApiProvider() {
        AIApi openAiApi1 = mock(AIApi.class);
        AIApi openAiApi2 = mock(AIApi.class);

        when(openAiApi1.aiProvider()).thenReturn(AIProvider.OPENAI);
        when(openAiApi2.aiProvider()).thenReturn(AIProvider.OPENAI);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new DefaultAiExecutor(List.of(openAiApi1, openAiApi2)));
        assertEquals("Duplicate AIApi for provider: OPENAI", ex.getMessage());
    }
}