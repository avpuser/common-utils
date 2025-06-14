package com.avpuser.ai.executor;

import com.avpuser.ai.AIApi;
import com.avpuser.ai.AIModel;
import com.avpuser.ai.AIProvider;
import com.avpuser.ai.AiResponseParser;
import com.avpuser.ai.deepseek.DeepSeekApi;
import com.avpuser.ai.openai.OpenAIApi;
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
    public String execute(AiPromptRequest request) {
        return executeWithStringResponse(request.getUserPrompt(), request.getSystemPrompt(), request.getModel());
    }

    private String executeWithStringResponse(String userPrompt, String systemPrompt, AIModel model) {
        AIApi api = aiApiMap.get(model.getProvider());
        if (api == null) {
            throw new IllegalArgumentException("Unsupported provider: " + model.getProvider());
        }

        String response = logAndExecCompletions(userPrompt, systemPrompt, model);
        if (api.returnsPlainText()) {
            logger.info("contentAsString (plain): {}", response);
            return response;
        }

        logger.info("jsonResponse: {}", response);
        String contentAsString = AiResponseParser.extractContentAsString(response);
        logger.info("contentAsString: {}", contentAsString);
        return contentAsString;
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