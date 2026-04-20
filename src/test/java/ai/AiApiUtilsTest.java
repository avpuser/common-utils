package ai;

import com.avpuser.ai.AIProvider;
import com.avpuser.ai.AiApiException;
import com.avpuser.ai.AiApiUtils;
import com.avpuser.ai.AiErrorType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class AiApiUtilsTest {

    @Test
    public void testExtractErrorMessage_validJsonWithMessage() {
        String json = """
                    {
                      "error": {
                        "message": "This model's maximum context length is 65536 tokens.",
                        "type": "invalid_request_error",
                        "code": "invalid_request_error"
                      }
                    }
                """;

        String result = AiApiUtils.extractApiErrorMessage(json);
        assertEquals("This model's maximum context length is 65536 tokens.", result);
    }

    @Test
    public void testExtractErrorMessage_validJsonWithoutMessage() {
        String json = """
                    {
                      "error": {
                        "type": "invalid_request_error",
                        "code": "invalid_request_error"
                      }
                    }
                """;

        String result = AiApiUtils.extractApiErrorMessage(json);
        assertEquals("Unknown error", result);
    }

    @Test
    public void testExtractErrorMessage_invalidJson() {
        String json = "not a json";

        String result = AiApiUtils.extractApiErrorMessage(json);
        assertEquals("Unknown API error", result);
    }

    @Test
    public void testExtractErrorMessage_validJsonWithoutErrorObject() {
        String json = """
                    {
                      "status": "error",
                      "reason": "invalid token"
                    }
                """;

        String result = AiApiUtils.extractApiErrorMessage(json);
        assertEquals("Unknown API error", result);
    }

    @Test
    public void checkAndThrowIfError_429QuotaExceeded_classifiesAsQuotaExceeded() {
        assertClassification(429, "quota exceeded", AiErrorType.QUOTA_EXCEEDED);
    }

    @Test
    public void checkAndThrowIfError_429TooManyRequests_classifiesAsRateLimit() {
        assertClassification(429, "too many requests", AiErrorType.RATE_LIMIT);
    }

    @Test
    public void checkAndThrowIfError_401_classifiesAsAuthError() {
        assertClassification(401, "unauthorized", AiErrorType.AUTH_ERROR);
    }

    @Test
    public void checkAndThrowIfError_403PolicyViolation_classifiesAsContentBlocked() {
        assertClassification(403, "policy violation", AiErrorType.CONTENT_BLOCKED);
    }

    @Test
    public void checkAndThrowIfError_403WithoutKeywords_classifiesAsPermissionDenied() {
        assertClassification(403, "forbidden", AiErrorType.PERMISSION_DENIED);
    }

    @Test
    public void checkAndThrowIfError_400BlockedBySafety_classifiesAsContentBlocked() {
        assertClassification(400, "blocked by safety", AiErrorType.CONTENT_BLOCKED);
    }

    @Test
    public void checkAndThrowIfError_400Generic_classifiesAsInvalidRequest() {
        assertClassification(400, "bad request", AiErrorType.INVALID_REQUEST);
    }

    @Test
    public void checkAndThrowIfError_404_classifiesAsNotFound() {
        assertClassification(404, "not found", AiErrorType.NOT_FOUND);
    }

    @Test
    public void checkAndThrowIfError_503_classifiesAsTemporaryUnavailable() {
        assertClassification(503, "service down", AiErrorType.TEMPORARY_UNAVAILABLE);
    }

    @Test
    public void checkAndThrowIfError_500_classifiesAsServerError() {
        assertClassification(500, "internal error", AiErrorType.SERVER_ERROR);
    }

    @Test
    public void checkAndThrowIfError_unknownStatus_classifiesAsUnknown() {
        assertClassification(418, "teapot", AiErrorType.UNKNOWN);
    }

    @Test
    public void checkAndThrowIfError_nullMessage_doesNotCrash() {
        HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(400);
        when(response.body()).thenReturn("{\"error\":{\"message\":null}}");

        AiApiException ex = assertThrows(AiApiException.class,
                () -> AiApiUtils.checkAndThrowIfError(response, AIProvider.OPENAI));
        assertEquals(AiErrorType.INVALID_REQUEST, ex.getErrorType());
    }

    private static void assertClassification(int status, String apiMessage, AiErrorType expected) {
        HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(errorJsonBody(apiMessage));

        AiApiException ex = assertThrows(AiApiException.class,
                () -> AiApiUtils.checkAndThrowIfError(response, AIProvider.OPENAI));
        assertEquals(expected, ex.getErrorType());
    }

    private static String errorJsonBody(String message) {
        String escaped = message.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"error\":{\"message\":\"" + escaped + "\"}}";
    }
}