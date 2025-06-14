package com.avpuser.ai;

public class AiApiException extends RuntimeException {
    private final int statusCode;
    private final String errorMessage;
    private final AIProvider aiProvider;

    public AiApiException(int statusCode, String errorMessage, AIProvider aiProvider) {
        super(aiProvider.name() + " Error " + statusCode + ": " + errorMessage);
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
        this.aiProvider = aiProvider;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public AIProvider getAIProvider() {
        return aiProvider;
    }
}