package com.avpuser.ai;

import com.avpuser.ai.executor.AiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GeminiAiResponseParser {

    private final static Logger logger = LogManager.getLogger(GeminiAiResponseParser.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static AiResponse extractAiResponse(String rawResponse, AIModel model) {
        logger.info("jsonResponse (Gemini): {}", rawResponse);

        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(rawResponse);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse Gemini JSON response, using raw response", e);
            return new AiResponse(rawResponse, model, null, null, null, null, null, null);
        }

        String contentResponse;
        try {
            contentResponse = extractContentAsString(rootNode);
            logger.info("contentAsString (Gemini): {}", contentResponse);
        } catch (Exception e) {
            logger.warn("Failed to extract content from Gemini JSON response, using raw response", e);
            contentResponse = rawResponse;
        }

        Integer inputTokens = extractInputTokens(rootNode);
        Integer outputTokens = extractOutputTokens(rootNode);
        Integer reasoningTokens = extractReasoningTokens(rootNode);
        Integer totalTokens = extractTotalTokens(rootNode);
        String providerModelName = extractProviderModelName(rootNode);
        String providerRequestId = extractProviderRequestId(rootNode);

        return new AiResponse(contentResponse, model, inputTokens, outputTokens, reasoningTokens, totalTokens, providerModelName, providerRequestId);
    }

    private static String extractContentAsString(JsonNode rootNode) {
        JsonNode candidates = rootNode.path("candidates");
        if (candidates.isMissingNode() || !candidates.isArray() || candidates.size() == 0) {
            throw new RuntimeException("Empty or unexpected Gemini response structure: missing candidates");
        }

        StringBuilder result = new StringBuilder();

        for (JsonNode candidate : candidates) {
            JsonNode content = candidate.path("content");
            if (content.isMissingNode()) {
                throw new RuntimeException("Empty or unexpected Gemini response structure: missing content");
            }

            JsonNode parts = content.path("parts");
            if (parts.isMissingNode() || !parts.isArray() || parts.size() == 0) {
                throw new RuntimeException("Empty or unexpected Gemini response structure: missing parts");
            }

            for (JsonNode part : parts) {
                // Most common case — text
                if (part.has("text")) {
                    String partText = part.get("text").asText();
                    if (!partText.isEmpty()) {
                        if (result.length() > 0) {
                            result.append("\n");
                        }
                        result.append(partText);
                    }
                    continue;
                }

                // If there is inline_data — skip it
                if (part.has("inline_data")) {
                    logger.debug("Gemini part contains inline_data — skipping.");
                    continue;
                }

                // If there is other content — log and skip
                logger.debug("Gemini part without text field: {}", part.toString());
            }
        }

        String contentText = result.toString();
        logger.info(contentText);
        return contentText;
    }

    private static Integer extractInputTokens(JsonNode rootNode) {
        try {
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
            logger.warn("Failed to extract input tokens from Gemini response", e);
            return null;
        }
    }

    private static Integer extractOutputTokens(JsonNode rootNode) {
        try {
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
            logger.warn("Failed to extract output tokens from Gemini response", e);
            return null;
        }
    }

    private static Integer extractReasoningTokens(JsonNode rootNode) {
        try {
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
            logger.warn("Failed to extract reasoning tokens from Gemini response", e);
            return null;
        }
    }

    private static Integer extractTotalTokens(JsonNode rootNode) {
        try {
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
            logger.warn("Failed to extract total tokens from Gemini response", e);
            return null;
        }
    }

    private static String extractProviderModelName(JsonNode rootNode) {
        try {
            JsonNode modelVersion = rootNode.path("modelVersion");
            if (modelVersion.isMissingNode()) {
                return null;
            }
            return modelVersion.asText();
        } catch (Exception e) {
            logger.warn("Failed to extract provider model name from Gemini response", e);
            return null;
        }
    }

    private static String extractProviderRequestId(JsonNode rootNode) {
        try {
            JsonNode responseId = rootNode.path("responseId");
            if (responseId.isMissingNode()) {
                return null;
            }
            return responseId.asText();
        } catch (Exception e) {
            logger.warn("Failed to extract provider request ID from Gemini response", e);
            return null;
        }
    }
}
