package utils;

import com.avpuser.utils.LogSanitizerUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogSanitizerUtilsTest {

    @Test
    void sanitizeExceptionMessage_nullReturnsEmpty() {
        assertEquals("", LogSanitizerUtils.sanitizeExceptionMessage(null));
    }

    @Test
    void sanitizeExceptionMessage_emptyReturnsEmpty() {
        assertEquals("", LogSanitizerUtils.sanitizeExceptionMessage(""));
    }

    @Test
    void sanitizeExceptionMessage_blankReturnsEmpty() {
        assertEquals("", LogSanitizerUtils.sanitizeExceptionMessage("   "));
    }

    @Test
    void sanitizeExceptionMessage_shortMessageUnchanged() {
        String msg = "Connection refused";
        assertEquals(msg, LogSanitizerUtils.sanitizeExceptionMessage(msg));
    }

    @Test
    void sanitizeExceptionMessage_longMessageTruncatedWithDefaultLength() {
        String msg = "a".repeat(250);
        String result = LogSanitizerUtils.sanitizeExceptionMessage(msg);
        assertEquals(203, result.length());
        assertEquals("...", result.substring(result.length() - 3));
        assertEquals(200, result.substring(0, result.length() - 3).length());
    }

    @Test
    void sanitizeExceptionMessage_withMaxLength() {
        String msg = "Hello world";
        assertEquals("Hello world", LogSanitizerUtils.sanitizeExceptionMessage(msg, 100));
        assertEquals("Hello...", LogSanitizerUtils.sanitizeExceptionMessage(msg, 5));
        assertEquals("", LogSanitizerUtils.sanitizeExceptionMessage(msg, 0));
    }

    @Test
    void sanitizeExceptionMessage_trimmedBeforeTruncate() {
        assertEquals("ab", LogSanitizerUtils.sanitizeExceptionMessage("  ab  ", 2));
    }

    @Test
    void sanitizeCause_nullReturnsNullString() {
        assertEquals("null", LogSanitizerUtils.sanitizeCause(null));
    }

    @Test
    void sanitizeCause_withMessage() {
        Exception e = new IllegalArgumentException("Invalid argument");
        assertEquals("IllegalArgumentException: Invalid argument", LogSanitizerUtils.sanitizeCause(e));
    }

    @Test
    void sanitizeCause_withNullMessage() {
        Exception e = new Exception();
        assertEquals("Exception: ", LogSanitizerUtils.sanitizeCause(e));
    }

    @Test
    void sanitizeCause_longMessageTruncated() {
        Exception e = new Exception("x".repeat(300));
        String result = LogSanitizerUtils.sanitizeCause(e);
        assertEquals("Exception: ", result.substring(0, 11));
        assertEquals("...", result.substring(result.length() - 3));
    }

    @Test
    void sanitizeCause_withMaxLength() {
        Exception e = new Exception("hello");
        assertEquals("Exception: he...", LogSanitizerUtils.sanitizeCause(e, 2));
    }

    @Test
    void maskUrlForLog_nullReturnsNullString() {
        assertEquals("null", LogSanitizerUtils.maskUrlForLog(null));
    }

    @Test
    void maskUrlForLog_noQueryUnchanged() {
        String url = "https://api.example.com/path";
        assertEquals(url, LogSanitizerUtils.maskUrlForLog(url));
    }

    @Test
    void maskUrlForLog_withQueryStripped() {
        String url = "https://s3.example.com/bucket/key?X-Amz-Signature=abc123&X-Amz-Credential=secret";
        assertEquals("https://s3.example.com/bucket/key?...", LogSanitizerUtils.maskUrlForLog(url));
    }

    @Test
    void maskUrlForLog_onlyQuery() {
        assertEquals("?...", LogSanitizerUtils.maskUrlForLog("?foo=bar"));
    }
}
