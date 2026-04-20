package com.avpuser.ai.google;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoogleAiApiKeyResolverTest {

    @Test
    void splitsPipeDelimitedKeys() {
        assertEquals(
                List.of("a", "b", "c"),
                GoogleAiApiKeyResolver.resolve("a|b|c", "ignored"));
    }

    @Test
    void trimsSegmentsAndSkipsEmpty() {
        assertEquals(
                List.of("k1", "k2"),
                GoogleAiApiKeyResolver.resolve(" k1 | | k2 ", ""));
    }

    @Test
    void deduplicatesPreservingOrder() {
        assertEquals(
                List.of("a", "b", "c"),
                GoogleAiApiKeyResolver.resolve("a|b|a|c|b", ""));
        assertEquals(
                List.of("dup"),
                GoogleAiApiKeyResolver.resolve("dup|dup|dup", "ignored"));
    }

    @Test
    void fallsBackToSingleWhenMultiBlank() {
        assertEquals(
                List.of("solo"),
                GoogleAiApiKeyResolver.resolve("  ", "solo"));
    }

    @Test
    void fallsBackToSingleWhenMultiNull() {
        assertEquals(
                List.of("solo"),
                GoogleAiApiKeyResolver.resolve(null, "solo"));
    }

    @Test
    void returnsEmptyWhenBothMissing() {
        assertEquals(List.of(), GoogleAiApiKeyResolver.resolve(null, null));
        assertEquals(List.of(), GoogleAiApiKeyResolver.resolve("", "  "));
    }
}
