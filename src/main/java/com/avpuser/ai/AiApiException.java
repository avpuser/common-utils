package com.avpuser.ai;

public class AiApiException extends RuntimeException {
    private final int statusCode;
    private final String errorMessage;
    private final AIProvider aiProvider;
    private final AiErrorType errorType;

    public AiApiException(int statusCode, String errorMessage, AIProvider aiProvider) {
        this(statusCode, errorMessage, aiProvider, AiErrorType.UNKNOWN);
    }

    public AiApiException(int statusCode, String errorMessage, AIProvider aiProvider, AiErrorType errorType) {
        super(aiProvider.name() + " Error " + statusCode + ": " + errorMessage);
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
        this.aiProvider = aiProvider;
        this.errorType = errorType;
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

    public AiErrorType getErrorType() {
        return errorType;
    }
}