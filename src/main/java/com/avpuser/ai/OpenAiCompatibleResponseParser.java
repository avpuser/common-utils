package com.avpuser.ai;

import com.avpuser.ai.executor.AiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OpenAiCompatibleResponseParser {

    private final static Logger logger = LogManager.getLogger(OpenAiCompatibleResponseParser.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static AiResponse extractAiResponse(String rawResponse, AIModel model) {
        logger.info("OpenAI response: responseLength={}", rawResponse != null ? rawResponse.length() : 0);

        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(rawResponse);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse OpenAI-compatible JSON response", e);
            return new AiResponse(rawResponse, model, null, null, null, null, null, null);
        }

        String contentResponse = extractContentAsString(rootNode);
        logger.info("OpenAI content: responseLength={}", contentResponse != null ? contentResponse.length() : 0);

        Integer inputTokens = extractInputTokens(rootNode);
        Integer outputTokens = extractOutputTokens(rootNode);
        Integer reasoningTokens = extractReasoningTokens(rootNode);
        Integer totalTokens = extractTotalTokens(rootNode);
        String providerModelName = extractProviderModelName(rootNode);
        String providerRequestId = extractProviderRequestId(rootNode);

        return new AiResponse(contentResponse, model, inputTokens, outputTokens, reasoningTokens, totalTokens, providerModelName, providerRequestId);
    }

    public static boolean isResponseCutOff(String jsonResponse) {
        try {
            JsonNode finishReasonNode = objectMapper
                    .readTree(jsonResponse)
                    .path("choices")
                    .get(0)
                    .path("finish_reason");

            return "length".equals(finishReasonNode.asText());
        } catch (Exception e) {
            logger.warn("Failed to parse finish_reason", e);
            return false;
        }
    }

    private static String extractContentAsString(JsonNode rootNode) {
        JsonNode messageNode = rootNode.path("choices").get(0).path("message").path("content");

        String content = messageNode.asText();
        logger.debug("OpenAI content length: {}", content != null ? content.length() : 0);
        return content;
    }

    private static Integer extractInputTokens(JsonNode rootNode) {
        try {
            JsonNode usage = rootNode.path("usage");
            if (usage.isMissingNode()) {
                return null;
            }
            JsonNode promptTokens = usage.path("prompt_tokens");
            if (promptTokens.isMissingNode()) {
                return null;
            }
            return promptTokens.asInt();
        } catch (Exception e) {
            logger.debug("Failed to extract input tokens from OpenAI-compatible response", e);
            return null;
        }
    }

    private static Integer extractOutputTokens(JsonNode rootNode) {
        try {
            JsonNode usage = rootNode.path("usage");
            if (usage.isMissingNode()) {
                return null;
            }
            JsonNode completionTokens = usage.path("completion_tokens");
            if (completionTokens.isMissingNode()) {
                return null;
            }
            return completionTokens.asInt();
        } catch (Exception e) {
            logger.debug("Failed to extract output tokens from OpenAI-compatible response", e);
            return null;
        }
    }

    private static Integer extractReasoningTokens(JsonNode rootNode) {
        try {
            JsonNode usage = rootNode.path("usage");
            if (usage.isMissingNode()) {
                return null;
            }
            JsonNode completionTokensDetails = usage.path("completion_tokens_details");
            if (completionTokensDetails.isMissingNode()) {
                return null;
            }
            JsonNode reasoningTokensNode = completionTokensDetails.path("reasoning_tokens");
            if (reasoningTokensNode.isMissingNode()) {
                return null;
            }
            return reasoningTokensNode.asInt();
        } catch (Exception e) {
            logger.debug("Failed to extract reasoning tokens from OpenAI-compatible response", e);
            return null;
        }
    }

    private static Integer extractTotalTokens(JsonNode rootNode) {
        try {
            JsonNode usage = rootNode.path("usage");
            if (usage.isMissingNode()) {
                return null;
            }
            JsonNode totalTokensNode = usage.path("total_tokens");
            if (totalTokensNode.isMissingNode()) {
                return null;
            }
            return totalTokensNode.asInt();
        } catch (Exception e) {
            logger.debug("Failed to extract total tokens from OpenAI-compatible response", e);
            return null;
        }
    }

    private static String extractProviderModelName(JsonNode rootNode) {
        try {
            JsonNode modelNode = rootNode.path("model");
            if (modelNode.isMissingNode()) {
                return null;
            }
            return modelNode.asText();
        } catch (Exception e) {
            logger.debug("Failed to extract provider model name from OpenAI-compatible response", e);
            return null;
        }
    }

    private static String extractProviderRequestId(JsonNode rootNode) {
        try {
            JsonNode idNode = rootNode.path("id");
            if (idNode.isMissingNode()) {
                return null;
            }
            return idNode.asText();
        } catch (Exception e) {
            logger.debug("Failed to extract provider request ID from OpenAI-compatible response", e);
            return null;
        }
    }
}
