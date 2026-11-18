package com.avpuser.utils;

import com.fasterxml.jackson.core.JsonParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Best-effort repair of LLM JSON that contains invalid backslash escapes inside string literals
 * (e.g. OCR-copied {@code \Я} instead of {@code \\Я}).
 *
 * <p>Does not alter valid JSON escapes, does not attempt structural recovery of truncated JSON,
 * and does not change {@link JsonUtils} global Jackson configuration.</p>
 */
public final class LlmJsonRepair {

    private static final Logger logger = LogManager.getLogger(LlmJsonRepair.class);

    /** Fixed error type for WARN logs — never derived from exception message content. */
    public static final String ERROR_TYPE_INVALID_ESCAPE = "INVALID_ESCAPE";

    private LlmJsonRepair() {
    }

    /**
     * Result of an escape-repair pass.
     *
     * @param json          repaired JSON text (or original when nothing changed)
     * @param fixedEscapes  number of invalid {@code \X} sequences rewritten as {@code \\X}
     */
    public record RepairResult(String json, int fixedEscapes) {
    }

    /**
     * Deserializes LLM JSON with standard Jackson first. Only if a {@link JsonParseException}
     * occurs, attempts invalid-escape repair inside string literals and retries once.
     *
     * @param json       raw LLM response (may include markdown fences; stripped via {@link JsonUtils})
     * @param clazz      target type
     * @param parserName short parser id for WARN logs (no payload logged)
     */
    public static <T> T deserializeWithEscapeRepair(String json, Class<T> clazz, String parserName) {
        try {
            return JsonUtils.deserializeJsonToObject(json, clazz);
        } catch (RuntimeException firstFailure) {
            JsonParseException parseException = findCause(firstFailure, JsonParseException.class);
            if (parseException == null) {
                throw firstFailure;
            }

            String toRepair = JsonUtils.stripJsonCodeBlock(json);
            RepairResult repaired = repairInvalidEscapes(toRepair);
            if (repaired.fixedEscapes() <= 0) {
                throw firstFailure;
            }

            try {
                T value = JsonUtils.deserializeJsonToObject(repaired.json(), clazz);
                logger.warn(
                        "LLM JSON escape repair succeeded: parser={}, errorType={}, fixedEscapes={}",
                        parserName,
                        ERROR_TYPE_INVALID_ESCAPE,
                        repaired.fixedEscapes());
                return value;
            } catch (RuntimeException secondFailure) {
                firstFailure.addSuppressed(secondFailure);
                throw firstFailure;
            }
        }
    }

    /**
     * Rewrites invalid {@code \X} inside JSON string literals to {@code \\X}, preserving the
     * backslash and following character in the eventual string value. Valid escapes are left unchanged.
     *
     * <p>A lone trailing {@code \} at end of input is left untouched (likely truncated LLM output).
     */
    public static RepairResult repairInvalidEscapes(String json) {
        if (json == null || json.isEmpty()) {
            return new RepairResult(json, 0);
        }

        StringBuilder out = new StringBuilder(json.length() + 16);
        int fixed = 0;
        boolean inString = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (!inString) {
                out.append(c);
                if (c == '"') {
                    inString = true;
                }
                continue;
            }

            if (c == '"') {
                out.append(c);
                inString = false;
                continue;
            }

            if (c != '\\') {
                out.append(c);
                continue;
            }

            if (i + 1 >= json.length()) {
                // Trailing lone backslash — treat as truncated/unrecoverable; do not rewrite.
                out.append('\\');
                break;
            }

            char next = json.charAt(i + 1);
            if (isSimpleValidEscape(next)) {
                out.append('\\').append(next);
                i++;
                continue;
            }

            if (next == 'u') {
                if (i + 5 < json.length()
                        && isHexDigit(json.charAt(i + 2))
                        && isHexDigit(json.charAt(i + 3))
                        && isHexDigit(json.charAt(i + 4))
                        && isHexDigit(json.charAt(i + 5))) {
                    out.append('\\').append('u')
                            .append(json.charAt(i + 2))
                            .append(json.charAt(i + 3))
                            .append(json.charAt(i + 4))
                            .append(json.charAt(i + 5));
                    i += 5;
                    continue;
                }
                out.append('\\').append('\\').append('u');
                fixed++;
                i++;
                continue;
            }

            // Invalid escape \X → \\X (preserves '\' + X in deserialized string value)
            out.append('\\').append('\\').append(next);
            fixed++;
            i++;
        }

        return new RepairResult(out.toString(), fixed);
    }

    private static boolean isSimpleValidEscape(char c) {
        return c == '"' || c == '\\' || c == '/' || c == 'b' || c == 'f'
                || c == 'n' || c == 'r' || c == 't';
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F');
    }

    private static <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
