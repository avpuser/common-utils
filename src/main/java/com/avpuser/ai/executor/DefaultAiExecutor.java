package com.avpuser.ai.executor;

import com.avpuser.ai.AIApi;
import com.avpuser.ai.AIModel;
import com.avpuser.ai.AIProvider;
import com.avpuser.ai.AiResponseParser;
import com.avpuser.ai.deepseek.DeepSeekApi;
import com.avpuser.ai.openai.OpenAIApi;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A default implementation of {@link AiExecutor} that handles plain text AI prompts
 * and delegates execution to the appropriate AI provider (e.g., OpenAI, DeepSeek).
 * <p>
 * This class supports execution of {@link AiPromptRequest} objects by serializing
 * the prompt to a string and passing it to the AI provider's completions API.
 * The AI's response is expected to be a JSON payload, from which the content is extracted.
 * </p>
 *
 * <p><b>Supported providers:</b></p>
 * <ul>
 *   <li>{@link OpenAIApi} for models like GPT-3.5 and GPT-4</li>
 *   <li>{@link DeepSeekApi} for DeepSeek-compatible models</li>
 * </ul>
 *
 * <p><b>Execution flow:</b></p>
 * <ol>
 *   <li>Validates the user and system prompts</li>
 *   <li>Executes a completion request through the provider based on {@link AIModel}</li>
 *   <li>Logs and parses the JSON response</li>
 *   <li>Returns the extracted textual result</li>
 * </ol>
 *
 * <p>Any blank prompt or unsupported provider will throw an {@link IllegalArgumentException}.</p>
 *
 * @see AiExecutor
 * @see AiPromptRequest
 * @see AIModel
 * @see OpenAIApi
 * @see DeepSeekApi
 * @see AiResponseParser
 */
public class DefaultAiExecutor implements AiExecutor {

    private final static Logger logger = LogManager.getLogger(DefaultAiExecutor.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<AIProvider, AIApi> aiApiMap;

    /**
     * Constructs a {@code DefaultAiExecutor} with access to specific provider APIs.
     */
    public DefaultAiExecutor(List<AIApi> aiApiList) {
        Map<AIProvider, AIApi> map = new EnumMap<>(AIProvider.class);

        for (AIApi api : aiApiList) {
            AIProvider provider = api.aiProvider();
            if (map.containsKey(provider)) {
                throw new IllegalArgumentException("Duplicate AIApi for provider: " + provider);
            }
            map.put(provider, api);
        }

        this.aiApiMap = Map.copyOf(map);
    }

    /**
     * Executes a raw prompt request using the appropriate provider and returns the extracted response.
     *
     * @param request The AI prompt request including model, user and system prompts.
     * @return Extracted content string from the provider's JSON response.
     * @throws IllegalArgumentException if the input is blank or the provider is unsupported
     */
    @Override
    public AiResponse execute(AiPromptRequest request) {
        AIApi api = aiApiMap.get(request.getModel().getProvider());
        if (api == null) {
            throw new IllegalArgumentException("Unsupported provider: " + request.getModel().getProvider());
        }

        String rawResponse = logAndExecCompletions(request.getUserPrompt(), request.getSystemPrompt(), request.getModel());
        
        Integer inputTokens = null;
        Integer outputTokens = null;
        String contentResponse;
        
        if (api.returnsPlainText()) {
            // For Gemini, rawResponse is now the full JSON response body
            if (request.getModel().getProvider() == AIProvider.GOOGLE) {
                try {
                    JsonNode rootNode = objectMapper.readTree(rawResponse);
                    
                    // Extract text content
                    JsonNode candidates = rootNode.path("candidates");
                    if (!candidates.isMissingNode() && candidates.isArray() && candidates.size() > 0) {
                        JsonNode firstCandidate = candidates.get(0);
                        JsonNode content = firstCandidate.path("content");
                        JsonNode parts = content.path("parts");
                        if (!parts.isMissingNode() && parts.isArray() && parts.size() > 0) {
                            JsonNode part = parts.get(0);
                            contentResponse = part.path("text").asText();
                        } else {
                            contentResponse = rawResponse;
                        }
                    } else {
                        contentResponse = rawResponse;
                    }
                    
                    // Extract usageMetadata
                    JsonNode usageMetadata = rootNode.path("usageMetadata");
                    if (!usageMetadata.isMissingNode()) {
                        JsonNode promptTokenCount = usageMetadata.path("promptTokenCount");
                        if (!promptTokenCount.isMissingNode()) {
                            inputTokens = promptTokenCount.asInt();
                        }
                        JsonNode completionTokenCount = usageMetadata.path("completionTokenCount");
                        if (!completionTokenCount.isMissingNode()) {
                            outputTokens = completionTokenCount.asInt();
                        }
                    }
                    
                    logger.info("contentAsString (Gemini): {}", contentResponse);
                } catch (Exception e) {
                    logger.debug("Failed to parse Gemini JSON response, treating as plain text", e);
                    contentResponse = rawResponse;
                }
            } else {
                logger.info("contentAsString (plain): {}", rawResponse);
                contentResponse = rawResponse;
            }
        } else {
            logger.info("jsonResponse: {}", rawResponse);
            contentResponse = AiResponseParser.extractContentAsString(rawResponse);
            logger.info("contentAsString: {}", contentResponse);
            
            // Parse usage from JSON response for OpenAI/DeepSeek
            try {
                JsonNode rootNode = objectMapper.readTree(rawResponse);
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
                }
            } catch (Exception e) {
                logger.debug("Failed to parse usage from JSON response", e);
            }
        }
        
        return new AiResponse(contentResponse, request.getModel(), inputTokens, outputTokens);
    }

    private String logAndExecCompletions(String userPrompt, String systemPrompt, AIModel model) {
        if (StringUtils.isBlank(userPrompt)) {
            throw new IllegalArgumentException("userPrompt must not be blank");
        }
        if (StringUtils.isBlank(systemPrompt)) {
            throw new IllegalArgumentException("systemPrompt must not be blank");
        }
        logger.info("userPrompt: {}", userPrompt);
        logger.info("systemPrompt: {}", systemPrompt);
        return execCompletions(userPrompt, systemPrompt, model);
    }

    private String execCompletions(String userPrompt, String systemPrompt, AIModel model) {
        AIApi api = aiApiMap.get(model.getProvider());
        if (api == null) {
            throw new IllegalArgumentException("Unsupported provider: " + model.getProvider());
        }
        return api.execCompletions(userPrompt, systemPrompt, model);
    }
}