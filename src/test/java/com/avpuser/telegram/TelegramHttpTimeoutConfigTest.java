package com.avpuser.telegram;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Minimal checks that Telegram HTTP clients configure connect/socket timeouts
 * and do not use the infinite defaults from {@code HttpClients.createDefault()}.
 */
class TelegramHttpTimeoutConfigTest {

    @Test
    void timeoutConstants() {
        assertEquals(10_000, TelegramHttpUtils.CONNECT_TIMEOUT_MS);
        assertEquals(30_000, TelegramHttpUtils.SOCKET_TIMEOUT_MS);
        assertEquals(3_000, TelegramHttpUtils.CONNECTION_REQUEST_TIMEOUT_MS);
    }

    @Test
    void textHttpClient_requestConfig() throws Exception {
        try (CloseableHttpClient client = TelegramHttpUtils.createHttpClient()) {
            RequestConfig config = requestConfig(client);
            assertEquals(TelegramHttpUtils.CONNECT_TIMEOUT_MS, config.getConnectTimeout());
            assertEquals(TelegramHttpUtils.SOCKET_TIMEOUT_MS, config.getSocketTimeout());
            assertEquals(TelegramHttpUtils.CONNECTION_REQUEST_TIMEOUT_MS, config.getConnectionRequestTimeout());
        }
    }

    @Test
    void classifyHttpError_distinguishesTimeoutKinds() {
        assertEquals("read_timeout",
                TelegramHttpUtils.classifyHttpError(new SocketTimeoutException("Read timed out")));
        assertEquals("connect_timeout",
                TelegramHttpUtils.classifyHttpError(new SocketTimeoutException("connect timed out")));
        assertEquals("connect_timeout",
                TelegramHttpUtils.classifyHttpError(new ConnectTimeoutException("Connect to api.telegram.org timed out")));
        assertEquals("connection_request_timeout",
                TelegramHttpUtils.classifyHttpError(new ConnectionPoolTimeoutException("Timeout waiting for connection")));
        assertEquals("connect_failed",
                TelegramHttpUtils.classifyHttpError(new ConnectException("Connection refused")));
        assertEquals("unknown_host",
                TelegramHttpUtils.classifyHttpError(new UnknownHostException("api.telegram.org")));
        assertEquals("read_timeout",
                TelegramHttpUtils.classifyHttpError(
                        new RuntimeException("wrapper", new SocketTimeoutException("Read timed out"))));
        assertEquals("IllegalStateException",
                TelegramHttpUtils.classifyHttpError(new IllegalStateException("other")));
    }

    private static RequestConfig requestConfig(CloseableHttpClient client) {
        assertInstanceOf(Configurable.class, client);
        return ((Configurable) client).getConfig();
    }
}
