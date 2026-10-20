package ai;

import com.avpuser.ai.AIModel;
import com.avpuser.ai.AIProvider;
import com.avpuser.ai.ChatCompletionApiClient;
import com.avpuser.ai.google.GoogleAIApi;
import com.avpuser.ai.openai.OpenAIApi;
import com.avpuser.test.MockTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Minimal checks that AI HTTP clients configure connect/request timeouts.
 */
@MockTest
class AiHttpTimeoutConfigTest {

    @Test
    void chatCompletionApiClient_timeouts() throws Exception {
        assertEquals(Duration.ofSeconds(10), ChatCompletionApiClient.CONNECT_TIMEOUT);
        assertEquals(Duration.ofSeconds(60), ChatCompletionApiClient.REQUEST_TIMEOUT);

        ChatCompletionApiClient client = new ChatCompletionApiClient("key", "https://example.test", AIProvider.OPENAI);
        assertEquals(Optional.of(Duration.ofSeconds(10)), httpClient(client, "client").connectTimeout());
    }

    @Test
    void googleAiApi_timeouts() throws Exception {
        assertEquals(Duration.ofSeconds(10), GoogleAIApi.CONNECT_TIMEOUT);
        assertEquals(Duration.ofSeconds(60), GoogleAIApi.REQUEST_TIMEOUT);
        assertEquals(Duration.ofSeconds(120), GoogleAIApi.OCR_REQUEST_TIMEOUT);

        GoogleAIApi api = new GoogleAIApi("key");
        assertEquals(Optional.of(Duration.ofSeconds(10)), httpClient(api, "client").connectTimeout());
    }

    @Test
    void openAiApi_ocrTimeouts() throws Exception {
        assertEquals(Duration.ofSeconds(10), OpenAIApi.CONNECT_TIMEOUT);
        assertEquals(Duration.ofSeconds(120), OpenAIApi.OCR_REQUEST_TIMEOUT);

        OpenAIApi api = new OpenAIApi("key");
        assertEquals(Optional.of(Duration.ofSeconds(10)), httpClient(api, "httpClient").connectTimeout());
    }

    @Test
    void chatCompletionApiClient_httpRequestTimeoutIs60s() throws Exception {
        ChatCompletionApiClient client = new ChatCompletionApiClient("key", "https://example.test", AIProvider.OPENAI);
        HttpRequest request = captureRequest(client, "client",
                () -> client.execCompletions("user", "system", AIModel.GPT_4O_MINI));
        assertEquals(Optional.of(Duration.ofSeconds(60)), request.timeout());
    }

    @Test
    void googleAiApi_textHttpRequestTimeoutIs60s() throws Exception {
        GoogleAIApi api = new GoogleAIApi("key");
        HttpRequest request = captureRequest(api, "client",
                () -> api.execCompletions("user", "system", AIModel.GEMINI_FLASH));
        assertEquals(Optional.of(Duration.ofSeconds(60)), request.timeout());
    }

    @Test
    void googleAiApi_ocrHttpRequestTimeoutIs120s() throws Exception {
        GoogleAIApi api = new GoogleAIApi("key");
        HttpRequest request = captureRequest(api, "client",
                () -> api.extractTextFromFile(new byte[]{1, 2, 3}, "image/png", "extract"));
        assertEquals(Optional.of(Duration.ofSeconds(120)), request.timeout());
    }

    @Test
    void openAiApi_ocrHttpRequestTimeoutIs120s() throws Exception {
        OpenAIApi api = new OpenAIApi("key");
        HttpRequest request = captureRequest(api, "httpClient",
                () -> api.extractTextFromFile(new byte[]{1, 2, 3}, "image/png", "extract"));
        assertEquals(Optional.of(Duration.ofSeconds(120)), request.timeout());
    }

    @SuppressWarnings("unchecked")
    private static HttpRequest captureRequest(Object target, String fieldName, Runnable action) throws Exception {
        AtomicReference<HttpRequest> captured = new AtomicReference<>();
        HttpClient mockClient = mock(HttpClient.class);
        HttpResponse<String> ok = mock(HttpResponse.class);
        when(ok.statusCode()).thenReturn(200);
        when(ok.body()).thenReturn("{}");
        when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer(invocation -> {
                    captured.set(invocation.getArgument(0));
                    return ok;
                });

        setHttpClient(target, fieldName, mockClient);
        action.run();

        HttpRequest request = captured.get();
        assertNotNull(request, "HttpClient.send was not called");
        return request;
    }

    private static void setHttpClient(Object target, String fieldName, HttpClient httpClient) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, httpClient);
    }

    private static HttpClient httpClient(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (HttpClient) field.get(target);
    }
}
