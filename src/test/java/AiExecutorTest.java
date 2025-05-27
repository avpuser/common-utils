import com.avpuser.gpt.AIModel;
import com.avpuser.gpt.AiExecutor;
import com.avpuser.gpt.deepseek.DeepSeekApi;
import com.avpuser.gpt.openai.OpenAIApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiExecutorTest {

    private OpenAIApi openAiApi;
    private DeepSeekApi deepSeekApi;
    private AiExecutor executor;

    @BeforeEach
    void setUp() {
        openAiApi = mock(OpenAIApi.class);
        deepSeekApi = mock(DeepSeekApi.class);
        executor = new AiExecutor(openAiApi, deepSeekApi);
    }

    @Test
    void shouldCallOpenAiApiAndReturnContent() {
        String prompt = "test prompt";
        String context = "test system context";
        AIModel model = AIModel.GPT_4;
        String jsonResponse = "{\"choices\":[{\"message\":{\"content\":\"Hello, world!\"}}]}";

        when(openAiApi.execCompletions(eq(prompt), eq(context), eq(model))).thenReturn(jsonResponse);

        String result = executor.executeAndExtractContent(prompt, context, model);
        assertEquals("Hello, world!", result);
    }

    @Test
    void shouldCallDeepSeekApiAndReturnContent() {
        String prompt = "deepseek test";
        String context = "deepseek context";
        AIModel model = AIModel.DEEPSEEK_CHAT;
        String jsonResponse = "{\"choices\":[{\"message\":{\"content\":\"Deep response\"}}]}";

        when(deepSeekApi.execCompletions(eq(prompt), eq(context), eq(model))).thenReturn(jsonResponse);

        String result = executor.executeAndExtractContent(prompt, context, model);
        assertEquals("Deep response", result);
    }

    @Test
    void shouldSerializeRequestAndDeserializeResponse() {
        DummyRequest request = new DummyRequest("test123");
        AIModel model = AIModel.GPT_4;
        String json = "{\"input\":\"test123\"}";
        String jsonResponse = "```json\n{\"value\":\"abc\"}\n```"; // имитируем ответ с json-блоком

        when(openAiApi.execCompletions(eq(json), any(), eq(model))).thenReturn(jsonResponse);

        DummyResponse response = executor.executeAndExtractContent(request, "some context", DummyResponse.class, model);
        assertNotNull(response);
        assertEquals("abc", response.getValue());
    }

    @Test
    void shouldThrowExceptionForBlankInput() {
        assertThrows(IllegalArgumentException.class, () ->
                executor.executeAndExtractContent("   ", "context", AIModel.GPT_4));
    }

    @Test
    void shouldThrowExceptionForBlankSystemContext() {
        assertThrows(IllegalArgumentException.class, () ->
                executor.executeAndExtractContent("prompt", "   ", AIModel.GPT_4));
    }

    static class DummyRequest {
        private String input;
        public DummyRequest(String input) { this.input = input; }
        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
    }

    static class DummyResponse {
        private String value;
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}