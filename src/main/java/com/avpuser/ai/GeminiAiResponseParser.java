package com.avpuser.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GeminiAiResponseParser {

    private final static Logger logger = LogManager.getLogger(GeminiAiResponseParser.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String extractContentAsString(String jsonResponse) {
        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(jsonResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse Gemini JSON response", e);
        }

        JsonNode candidates = rootNode.path("candidates");
        if (candidates.isMissingNode() || !candidates.isArray() || candidates.size() == 0) {
            throw new RuntimeException("Empty or unexpected Gemini response structure: missing candidates");
        }

        JsonNode firstCandidate = candidates.get(0);
        JsonNode content = firstCandidate.path("content");
        if (content.isMissingNode()) {
            throw new RuntimeException("Empty or unexpected Gemini response structure: missing content");
        }

        JsonNode parts = content.path("parts");
        if (parts.isMissingNode() || !parts.isArray() || parts.size() == 0) {
            throw new RuntimeException("Empty or unexpected Gemini response structure: missing parts");
        }

        JsonNode part = parts.get(0);
        JsonNode text = part.path("text");
        if (text.isMissingNode()) {
            throw new RuntimeException("Empty or unexpected Gemini response structure: missing text");
        }

        String contentText = text.asText();
        logger.info(contentText);
        return contentText;
    }

    public static Integer extractInputTokens(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode usageMetadata = rootNode.path("usageMetadata");
            if (usageMetadata.isMissingNode()) {
                return null;
            }
            JsonNode promptTokenCount = usageMetadata.path("promptTokenCount");
            if (promptTokenCount.isMissingNode()) {
                return null;
            }
            return promptTokenCount.asInt();
        } catch (Exception e) {
            logger.debug("Failed to extract input tokens from Gemini response", e);
            return null;
        }
    }

    public static Integer extractOutputTokens(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode usageMetadata = rootNode.path("usageMetadata");
            if (usageMetadata.isMissingNode()) {
                return null;
            }
            JsonNode completionTokenCount = usageMetadata.path("candidatesTokenCount");
            if (completionTokenCount.isMissingNode()) {
                return null;
            }
            return completionTokenCount.asInt();
        } catch (Exception e) {
            logger.debug("Failed to extract output tokens from Gemini response", e);
            return null;
        }
    }

    public static Integer extractReasoningTokens(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode usageMetadata = rootNode.path("usageMetadata");
            if (usageMetadata.isMissingNode()) {
                return null;
            }
            JsonNode thoughtsTokenCount = usageMetadata.path("thoughtsTokenCount");
            if (thoughtsTokenCount.isMissingNode()) {
                return null;
            }
            return thoughtsTokenCount.asInt();
        } catch (Exception e) {
            logger.debug("Failed to extract reasoning tokens from Gemini response", e);
            return null;
        }
    }

    public static Integer extractTotalTokens(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode usageMetadata = rootNode.path("usageMetadata");
            if (usageMetadata.isMissingNode()) {
                return null;
            }
            JsonNode totalTokenCount = usageMetadata.path("totalTokenCount");
            if (totalTokenCount.isMissingNode()) {
                return null;
            }
            return totalTokenCount.asInt();
        } catch (Exception e) {
            logger.debug("Failed to extract total tokens from Gemini response", e);
            return null;
        }
    }

    public static String extractProviderModelName(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode modelVersion = rootNode.path("modelVersion");
            if (modelVersion.isMissingNode()) {
                return null;
            }
            return modelVersion.asText();
        } catch (Exception e) {
            logger.debug("Failed to extract provider model name from Gemini response", e);
            return null;
        }
    }

    public static String extractProviderRequestId(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode responseId = rootNode.path("responseId");
            if (responseId.isMissingNode()) {
                return null;
            }
            return responseId.asText();
        } catch (Exception e) {
            logger.debug("Failed to extract provider request ID from Gemini response", e);
            return null;
        }
    }
}

