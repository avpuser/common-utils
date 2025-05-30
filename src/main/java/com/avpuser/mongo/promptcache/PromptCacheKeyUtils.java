package com.avpuser.mongo.promptcache;

import com.avpuser.ai.AIModel;
import org.apache.commons.codec.digest.DigestUtils;

public class PromptCacheKeyUtils {

    public static String buildHashKey(String promptType, String userPrompt, String systemPrompt, AIModel model) {
        String base = String.join("::",
                promptType,
                model.getModelName(),
                normalize(userPrompt),
                normalize(systemPrompt)
        );
        return DigestUtils.sha256Hex(base);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        return text
                .toLowerCase()
                // Replace non-breaking and special spaces with regular space
                .replaceAll("[\\u00A0\\u2007\\u202F]", " ")
                // Collapse multiple whitespace characters into a single space
                .replaceAll("\\s+", " ")
                // Trim leading and trailing spaces
                .trim();
    }

}
