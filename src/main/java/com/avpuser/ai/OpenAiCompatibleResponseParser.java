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

    public static String extractContentAsString(String jsonResponse) {

        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(jsonResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        JsonNode messageNode = rootNode.path("choices").get(0).path("message").path("content");

        String content = messageNode.asText();
        logger.info(content);
        return content;
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

    public static Integer extractInputTokens(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
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

    public static Integer extractOutputTokens(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
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

    public static Integer extractReasoningTokens(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
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

    public static Integer extractTotalTokens(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
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

    public static String extractProviderModelName(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
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

    public static String extractProviderRequestId(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
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

    public static AiResponse extractAiResponse(String rawResponse, AIModel model) {
        logger.info("jsonResponse: {}", rawResponse);

        String contentResponse = extractContentAsString(rawResponse);
        logger.info("contentAsString: {}", contentResponse);

        Integer inputTokens = extractInputTokens(rawResponse);
        Integer outputTokens = extractOutputTokens(rawResponse);
        Integer reasoningTokens = extractReasoningTokens(rawResponse);
        Integer totalTokens = extractTotalTokens(rawResponse);
        String providerModelName = extractProviderModelName(rawResponse);
        String providerRequestId = extractProviderRequestId(rawResponse);

        return new AiResponse(contentResponse, model, inputTokens, outputTokens, reasoningTokens, totalTokens, providerModelName, providerRequestId);
    }
}
