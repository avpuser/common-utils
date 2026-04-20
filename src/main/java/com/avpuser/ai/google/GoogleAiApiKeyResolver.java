package com.avpuser.ai.google;

import java.util.Arrays;
import java.util.List;

public final class GoogleAiApiKeyResolver {

    private GoogleAiApiKeyResolver() {
    }

    /**
     * Builds a unique API key list from pipe-delimited {@code apiKeysRaw}. If that yields no keys,
     * falls back to {@code singleApiKey} when non-blank.
     */
    public static List<String> resolve(String apiKeysRaw, String singleApiKey) {
        if (apiKeysRaw != null && !apiKeysRaw.isBlank()) {
            List<String> keys = Arrays.stream(apiKeysRaw.split("\\|"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .toList();
            if (!keys.isEmpty()) {
                return keys;
            }
        }
        if (singleApiKey != null && !singleApiKey.isBlank()) {
            return List.of(singleApiKey.trim());
        }
        return List.of();
    }
}
