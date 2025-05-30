package ai;

import com.avpuser.ai.AIModel;
import com.avpuser.ai.deepseek.DeepSeekApi;
import com.avpuser.ai.executor.DefaultAiExecutor;
import com.avpuser.ai.executor.TypedPromptRequest;
import com.avpuser.ai.openai.OpenAIApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DefaultAiExecutorTest {

    private OpenAIApi openAIApi;
    private DeepSeekApi deepSeekApi;
    private DefaultAiExecutor executor;

    @BeforeEach
    void setup() {
        openAIApi = mock(OpenAIApi.class);
        deepSeekApi = mock(DeepSeekApi.class);
        executor = new DefaultAiExecutor(openAIApi, deepSeekApi);
    }

    @Test
    void testExecuteAndExtractContent_StringToString_OpenAI() {
        String prompt = "What is the capital of France?";
        String systemContext = "You are a helpful assistant.";
        String rawJsonResponse = "{\"choices\":[{\"message\":{\"content\":\"Paris\"}}]}";

        AIModel model = AIModel.GPT_4;

        when(openAIApi.execCompletions(prompt, systemContext, model)).thenReturn(rawJsonResponse);

        TypedPromptRequest<String, String> request = TypedPromptRequest.of(
                prompt, systemContext, String.class, model, "trivia-capital");

        String result = executor.executeAndExtractContent(request);
        assertEquals("Paris", result);
        verify(openAIApi).execCompletions(prompt, systemContext, model);
    }

    @Test
    void testExecuteAndExtractContent_JsonRequestToResponse_DeepSeek() {
        DummyInput input = new DummyInput("value");
        String expectedJson = "{\"response\":\"ok\"}";
        String systemContext = "You are a JSON processor.";

        AIModel model = AIModel.DEEPSEEK_CHAT;

        when(deepSeekApi.execCompletions(anyString(), eq(systemContext), eq(model))).thenReturn(expectedJson);

        TypedPromptRequest<DummyInput, DummyOutput> request =  TypedPromptRequest.of(
                input, systemContext, DummyOutput.class, model, "dummy"
        );

        DummyOutput result = executor.executeAndExtractContent(request);
        assertEquals("ok", result.getResponse());
        verify(deepSeekApi).execCompletions(anyString(), eq(systemContext), eq(model));
    }

    static class DummyInput {
        public String value;

        public DummyInput(String value) {
            this.value = value;
        }
    }

    static class DummyOutput {
        private String response;

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }
    }
}