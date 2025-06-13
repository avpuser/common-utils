package com.avpuser.ai;

public class AiApiException extends RuntimeException {
    private final int statusCode;
    private final String errorMessage;

    public AiApiException(int statusCode, String errorMessage) {
        super("DeepSeek API Error " + statusCode + ": " + errorMessage);
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}