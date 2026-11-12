package com.avpuser.telegram;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Minimal checks that Telegram HTTP clients configure connect/socket timeouts
 * and do not use the infinite defaults from {@code HttpClients.createDefault()}.
 */
class TelegramHttpTimeoutConfigTest {

    @Test
    void timeoutConstants() {
        assertEquals(5_000, TelegramHttpUtils.CONNECT_TIMEOUT_MS);
        assertEquals(10_000, TelegramHttpUtils.SOCKET_TIMEOUT_MS);
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

    private static RequestConfig requestConfig(CloseableHttpClient client) {
        assertInstanceOf(Configurable.class, client);
        return ((Configurable) client).getConfig();
    }
}
