package com.avpuser.utils;

/**
 * Utilities for sanitizing data before logging to avoid PII/PHI/secrets in logs.
 */
public final class LogSanitizerUtils {

    public static final int DEFAULT_MAX_LENGTH = 200;

    private LogSanitizerUtils() {
    }

    /**
     * Truncates and trims exception message for safe logging. Use instead of raw getMessage().
     *
     * @param message exception message (may be null)
     * @return empty string if null/empty, else trimmed message truncated to {@link #DEFAULT_MAX_LENGTH} chars
     */
    public static String sanitizeExceptionMessage(String message) {
        return sanitizeExceptionMessage(message, DEFAULT_MAX_LENGTH);
    }

    /**
     * Truncates and trims exception message for safe logging.
     *
     * @param message   exception message (may be null)
     * @param maxLength maximum length before truncation (append "...")
     * @return empty string if null/empty, else trimmed message truncated to maxLength chars
     */
    public static String sanitizeExceptionMessage(String message, int maxLength) {
        if (message == null || message.isEmpty() || maxLength <= 0) {
            return "";
        }
        String trimmed = message.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "...";
    }

    /**
     * Builds a safe string from a throwable for logging: class name + sanitized message.
     * Use instead of t.toString() or t.getMessage() to avoid PII/PHI in logs.
     *
     * @param t throwable (may be null)
     * @return "null" if t is null, else "ClassName: sanitizedMessage"
     */
    public static String sanitizeCause(Throwable t) {
        return sanitizeCause(t, DEFAULT_MAX_LENGTH);
    }

    /**
     * Builds a safe string from a throwable for logging: class name + sanitized message.
     *
     * @param t         throwable (may be null)
     * @param maxLength maximum length for the message part
     * @return "null" if t is null, else "ClassName: sanitizedMessage"
     */
    public static String sanitizeCause(Throwable t, int maxLength) {
        if (t == null) {
            return "null";
        }
        String safe = sanitizeExceptionMessage(t.getMessage(), maxLength);
        return t.getClass().getSimpleName() + ": " + safe;
    }

    /**
     * Masks URL for logging by stripping query string (e.g. presigned params, tokens).
     *
     * @param url URL (may be null)
     * @return "null" if url is null, else URL up to (excluding) first '?', or "?..."
     */
    public static String maskUrlForLog(String url) {
        if (url == null) {
            return "null";
        }
        if (url.startsWith("?")) {
            return "?...";
        }
        int q = url.indexOf('?');
        return q > 0 ? url.substring(0, q) + "?..." : url;
    }
}
