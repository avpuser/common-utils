package com.avpuser.ai.executor;

import com.avpuser.ai.AIModel;
import com.avpuser.ai.AiResponseParser;
import com.avpuser.ai.deepseek.DeepSeekApi;
import com.avpuser.ai.openai.OpenAIApi;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Basic implementation of {@link AiExecutor} that supports both raw string prompts
 * and structured JSON-based AI requests.
 *
 * <p>This executor selects the appropriate method to handle the request based on
 * the runtime type of the request and expected response:</p>
 *
 * <ul>
 *     <li>If both request and response are strings, it treats the input as a raw prompt and returns the text content.</li>
 *     <li>Otherwise, it serializes the input to JSON, sends it to the AI, and deserializes the response to the expected type.</li>
 * </ul>
 *
 * <p>This class supports multiple AI providers (e.g., OpenAI, DeepSeek), selected based on the {@link AIModel} configuration.</p>
 *
 * @see AiExecutor
 * @see AiPromptRequest
 * @see OpenAIApi
 * @see DeepSeekApi
 */
public class DefaultAiExecutor implements AiExecutor {

    private final static Logger logger = LogManager.getLogger(DefaultAiExecutor.class);

    private final OpenAIApi openAiApi;
    private final DeepSeekApi deepSeekApi;

    public DefaultAiExecutor(OpenAIApi openAiApi, DeepSeekApi deepSeekApi) {
        this.openAiApi = openAiApi;
        this.deepSeekApi = deepSeekApi;
    }

    /**
     * Sends a raw user input string and system context to the AI provider,
     * executes the completion request in a separate thread, tracks progress in parallel,
     * and returns the extracted textual content from the JSON response.
     *
     * @param userPrompt   The raw prompt or input string for the AI.
     * @param systemPrompt The optional system context or prompt instructions.
     * @return Extracted content string from the AI's JSON response.
     */
    private String executeWithStringResponse(String userPrompt, String systemPrompt, AIModel model) {
        String jsonResponse = logAndExecCompletions(userPrompt, systemPrompt, model);
        logger.info("jsonResponse: " + jsonResponse);
        String contentAsString = AiResponseParser.extractContentAsString(jsonResponse);
        logger.info("contentAsString: " + contentAsString);
        return contentAsString;
    }

    private String logAndExecCompletions(String userInput, String systemContext, AIModel model) {
        if (StringUtils.isBlank(userInput)) {
            throw new IllegalArgumentException("userInput");
        }
        if (StringUtils.isBlank(systemContext)) {
            throw new IllegalArgumentException("systemContext");
        }
        logger.info("userInput: " + userInput);
        logger.info("systemContext: " + systemContext);
        String jsonResponse = execCompletions(userInput, systemContext, model);
        logger.info("jsonResponse: " + jsonResponse);
        return jsonResponse;
    }

    private String execCompletions(String userInput, String systemContext, AIModel model) {
        return switch (model.getProvider()) {
            case OPENAI -> openAiApi.execCompletions(userInput, systemContext, model);
            case DEEPSEEK -> deepSeekApi.execCompletions(userInput, systemContext, model);
            default -> throw new IllegalArgumentException("Unsupported provider: " + model.getProvider());
        };
    }

    @Override
    public String execute(AiPromptRequest request) {
        return executeWithStringResponse(request.getUserPrompt(), request.getSystemPrompt(), request.getModel());
    }
}
