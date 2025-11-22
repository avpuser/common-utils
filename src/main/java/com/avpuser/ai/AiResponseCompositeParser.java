package com.avpuser.ai;

import com.avpuser.ai.executor.AiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AiResponseCompositeParser {

    private final static Logger logger = LogManager.getLogger(AiResponseCompositeParser.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static AiResponse extractAiResponse(AIProvider provider, String rawResponse, AIModel model) {
        if (provider == AIProvider.GOOGLE) {
            return extractGeminiResponse(rawResponse, model);
        } else {
            return extractOpenAiCompatibleResponse(rawResponse, model);
        }
    }

    private static AiResponse extractGeminiResponse(String rawResponse, AIModel model) {
        logger.info("jsonResponse (Gemini): {}", rawResponse);
        
        String contentResponse;
        try {
            contentResponse = GeminiAiResponseParser.extractContentAsString(rawResponse);
            logger.info("contentAsString (Gemini): {}", contentResponse);
        } catch (Exception e) {
            logger.warn("Failed to parse Gemini JSON response, using raw response", e);
            contentResponse = rawResponse;
        }

        Integer inputTokens = GeminiAiResponseParser.extractInputTokens(rawResponse);
        Integer outputTokens = GeminiAiResponseParser.extractOutputTokens(rawResponse);
        Integer reasoningTokens = GeminiAiResponseParser.extractReasoningTokens(rawResponse);
        Integer totalTokens = GeminiAiResponseParser.extractTotalTokens(rawResponse);
        String providerModelName = GeminiAiResponseParser.extractProviderModelName(rawResponse);
        String providerRequestId = GeminiAiResponseParser.extractProviderRequestId(rawResponse);

        return new AiResponse(contentResponse, model, inputTokens, outputTokens, reasoningTokens, totalTokens, providerModelName, providerRequestId);
    }

    private static AiResponse extractOpenAiCompatibleResponse(String rawResponse, AIModel model) {
        logger.info("jsonResponse: {}", rawResponse);
        
        String contentResponse = OpenAiCompatibleResponseParser.extractContentAsString(rawResponse);
        logger.info("contentAsString: {}", contentResponse);

        Integer inputTokens = null;
        Integer outputTokens = null;
        Integer reasoningTokens = null;
        Integer totalTokens = null;
        String providerModelName = null;
        String providerRequestId = null;

        try {
            JsonNode rootNode = objectMapper.readTree(rawResponse);
            
            JsonNode modelNode = rootNode.path("model");
            if (!modelNode.isMissingNode()) {
                providerModelName = modelNode.asText();
            }
            
            JsonNode idNode = rootNode.path("id");
            if (!idNode.isMissingNode()) {
                providerRequestId = idNode.asText();
            }
            
            JsonNode usage = rootNode.path("usage");
            if (!usage.isMissingNode()) {
                JsonNode promptTokens = usage.path("prompt_tokens");
                if (!promptTokens.isMissingNode()) {
                    inputTokens = promptTokens.asInt();
                }
                JsonNode completionTokens = usage.path("completion_tokens");
                if (!completionTokens.isMissingNode()) {
                    outputTokens = completionTokens.asInt();
                }
                JsonNode totalTokensNode = usage.path("total_tokens");
                if (!totalTokensNode.isMissingNode()) {
                    totalTokens = totalTokensNode.asInt();
                }
                JsonNode completionTokensDetails = usage.path("completion_tokens_details");
                if (!completionTokensDetails.isMissingNode()) {
                    JsonNode reasoningTokensNode = completionTokensDetails.path("reasoning_tokens");
                    if (!reasoningTokensNode.isMissingNode()) {
                        reasoningTokens = reasoningTokensNode.asInt();
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to parse usage from JSON response", e);
        }

        return new AiResponse(contentResponse, model, inputTokens, outputTokens, reasoningTokens, totalTokens, providerModelName, providerRequestId);
    }
}

