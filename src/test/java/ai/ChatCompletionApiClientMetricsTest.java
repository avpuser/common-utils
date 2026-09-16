package ai;

import ai.metrics.RecordingAiMetrics;
import com.avpuser.ai.AIModel;
import com.avpuser.ai.AIProvider;
import com.avpuser.ai.AiApiException;
import com.avpuser.ai.ChatCompletionApiClient;
import com.avpuser.ai.metrics.AiMetricsTags;
import com.avpuser.test.MockTest;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MockTest
class ChatCompletionApiClientMetricsTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void openAiSuccess_recordsOneAttemptWithProviderAndModel() throws Exception {
        server = startServer(200, "{\"choices\":[{\"message\":{\"content\":\"hi\"}}]}");
        RecordingAiMetrics metrics = new RecordingAiMetrics();
        ChatCompletionApiClient client = new ChatCompletionApiClient(
                "key", baseUrl(), AIProvider.OPENAI, metrics);

        client.execCompletions("user", "system", AIModel.GPT_4O_MINI);

        assertEquals(1, metrics.attempts.size());
        assertEquals(new RecordingAiMetrics.AttemptEvent(
                "OPENAI", "GPT_4O_MINI", AiMetricsTags.ACCOUNT_NONE,
                AiMetricsTags.RESULT_SUCCESS, AiMetricsTags.ERROR_TYPE_NONE), metrics.attempts.get(0));
    }

    @Test
    void deepSeekHttpError_recordsOneAttemptWithDeepSeekProvider() throws Exception {
        server = startServer(429, "{\"error\":{\"message\":\"rate limit\"}}");
        RecordingAiMetrics metrics = new RecordingAiMetrics();
        ChatCompletionApiClient client = new ChatCompletionApiClient(
                "key", baseUrl(), AIProvider.DEEPSEEK, metrics);

        assertThrows(AiApiException.class,
                () -> client.execCompletions("user", "system", AIModel.DEEPSEEK_CHAT));

        assertEquals(1, metrics.attempts.size());
        assertEquals("DEEPSEEK", metrics.attempts.get(0).provider());
        assertEquals("DEEPSEEK_CHAT", metrics.attempts.get(0).model());
        assertEquals(AiMetricsTags.ACCOUNT_NONE, metrics.attempts.get(0).account());
        assertEquals(AiMetricsTags.RESULT_ERROR, metrics.attempts.get(0).result());
        assertEquals("rate_limit", metrics.attempts.get(0).errorType());
    }

    @Test
    void networkFailure_recordsExactlyOneAttempt() {
        RecordingAiMetrics metrics = new RecordingAiMetrics();
        ChatCompletionApiClient client = new ChatCompletionApiClient(
                "key", "http://127.0.0.1:1/v1/chat/completions", AIProvider.OPENAI, metrics);

        assertThrows(RuntimeException.class,
                () -> client.execCompletions("user", "system", AIModel.GPT_4O_MINI));

        assertEquals(1, metrics.attempts.size());
        assertEquals(AiMetricsTags.RESULT_ERROR, metrics.attempts.get(0).result());
        assertEquals("network_error", metrics.attempts.get(0).errorType());
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";
    }

    private HttpServer startServer(int status, String body) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/v1/chat/completions", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        httpServer.start();
        return httpServer;
    }
}
