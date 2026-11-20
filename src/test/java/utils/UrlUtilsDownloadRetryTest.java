package utils;

import com.avpuser.test.MockTest;
import com.avpuser.utils.UrlUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

@MockTest
class UrlUtilsDownloadRetryTest {

    @Test
    void isRetryable_socketTimeout_isRetryable() {
        RuntimeException wrapped = new RuntimeException(new SocketTimeoutException("Connect timed out"));
        assertTrue(UrlUtils.isRetryableDownloadFailure(wrapped));
    }

    @Test
    void isRetryable_connectException_isRetryable() {
        assertTrue(UrlUtils.isRetryableDownloadFailure(new RuntimeException(new ConnectException("Connection refused"))));
    }

    @Test
    void isRetryable_unknownHost_isRetryable() {
        assertTrue(UrlUtils.isRetryableDownloadFailure(new RuntimeException(new UnknownHostException("dns"))));
    }

    @Test
    void isRetryable_http503_isRetryable() {
        assertTrue(UrlUtils.isRetryableDownloadFailure(
                new RuntimeException(new IOException("HTTP error code: 503"))));
    }

    @Test
    void isRetryable_http429_isRetryable() {
        assertTrue(UrlUtils.isRetryableDownloadFailure(
                new RuntimeException(new IOException("HTTP error code: 429"))));
    }

    @Test
    void isRetryable_http408_isRetryable() {
        assertTrue(UrlUtils.isRetryableDownloadFailure(
                new RuntimeException(new IOException("HTTP error code: 408"))));
    }

    @Test
    void isRetryable_http403_isNotRetryable() {
        assertFalse(UrlUtils.isRetryableDownloadFailure(
                new RuntimeException(new IOException("HTTP error code: 403"))));
    }

    @Test
    void isRetryable_http404_isNotRetryable() {
        assertFalse(UrlUtils.isRetryableDownloadFailure(
                new RuntimeException(new IOException("HTTP error code: 404"))));
    }

    @Test
    void isRetryable_http400_isNotRetryable() {
        assertFalse(UrlUtils.isRetryableDownloadFailure(
                new RuntimeException(new IOException("HTTP error code: 400"))));
    }

    @Test
    void extractHttpErrorCode_parsesFromCauseChain() {
        OptionalInt code = UrlUtils.extractHttpErrorCode(
                new RuntimeException(new IOException("HTTP error code: 502")));
        assertTrue(code.isPresent());
        assertEquals(502, code.getAsInt());
    }

    @Test
    void extractHttpErrorCode_absentWhenNoHttpMessage() {
        assertTrue(UrlUtils.extractHttpErrorCode(new RuntimeException(new SocketTimeoutException("x"))).isEmpty());
    }
}
