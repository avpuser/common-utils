package com.avpuser.telegram;

public class TelegramRateLimitException extends RuntimeException {

    private final int retryAfterSeconds;

    public TelegramRateLimitException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}