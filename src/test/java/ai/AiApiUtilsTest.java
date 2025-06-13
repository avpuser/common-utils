package ai;

import com.avpuser.ai.AiApiUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}