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
        assertEquals(1_000, TelegramHttpUtils.CONNECT_TIMEOUT_MS);
        assertEquals(1_000, TelegramHttpUtils.CONNECTION_REQUEST_TIMEOUT_MS);
        assertEquals(3_000, TelegramHttpUtils.TEXT_SOCKET_TIMEOUT_MS);
        assertEquals(60_000, TelegramHttpUtils.AUDIO_SOCKET_TIMEOUT_MS);
    }

    @Test
    void textHttpClient_requestConfig() throws Exception {
        try (CloseableHttpClient client = TelegramHttpUtils.createHttpClient()) {
            RequestConfig config = requestConfig(client);
            assertEquals(TelegramHttpUtils.CONNECT_TIMEOUT_MS, config.getConnectTimeout());
            assertEquals(TelegramHttpUtils.CONNECTION_REQUEST_TIMEOUT_MS, config.getConnectionRequestTimeout());
            assertEquals(TelegramHttpUtils.TEXT_SOCKET_TIMEOUT_MS, config.getSocketTimeout());
        }
    }

    @Test
    void audioHttpClient_overridesOnlySocketTimeout() throws Exception {
        try (CloseableHttpClient client = TelegramHttpUtils.createAudioHttpClient()) {
            RequestConfig config = requestConfig(client);
            assertEquals(TelegramHttpUtils.CONNECT_TIMEOUT_MS, config.getConnectTimeout());
            assertEquals(TelegramHttpUtils.CONNECTION_REQUEST_TIMEOUT_MS, config.getConnectionRequestTimeout());
            assertEquals(TelegramHttpUtils.AUDIO_SOCKET_TIMEOUT_MS, config.getSocketTimeout());
        }
    }

    private static RequestConfig requestConfig(CloseableHttpClient client) {
        assertInstanceOf(Configurable.class, client);
        return ((Configurable) client).getConfig();
    }
}
