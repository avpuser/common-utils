package com.avpuser.gpt.executor;

import com.avpuser.gpt.AIModel;
import com.avpuser.gpt.GptResponseParser;
import com.avpuser.gpt.deepseek.DeepSeekApi;
import com.avpuser.gpt.openai.OpenAIApi;
import com.avpuser.utils.JsonUtils;
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
 * @see TypedPromptRequest
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
     * Serializes the given request object to JSON, sends it to the AI provider along with the system context,
     * tracks progress, and deserializes the AI response into the specified response class.
     *
     * @param request       The request object to be serialized and sent.
     * @param systemContext The optional system context or prompt instructions.
     * @param responseClass The target class to deserialize the AI response into.
     * @return Deserialized response of type TResponse.
     */
    private <TRequest, TResponse> TResponse executeAndExtractContent(TRequest request, String systemContext, Class<TResponse> responseClass, AIModel model) {
        String userInput = JsonUtils.toJson(request);
        String aiResponse = JsonUtils.stripJsonCodeBlock(logAndExecCompletions(userInput, systemContext, model));
        return JsonUtils.deserializeJsonToObject(aiResponse, responseClass);
    }

    /**
     * Sends a raw user input string and system context to the AI provider,
     * executes the completion request in a separate thread, tracks progress in parallel,
     * and returns the extracted textual content from the JSON response.
     *
     * @param userInput     The raw prompt or input string for the AI.
     * @param systemContext The optional system context or prompt instructions.
     * @return Extracted content string from the AI's JSON response.
     */
    private String executeAndExtractContent(String userInput, String systemContext, AIModel model) {
        String jsonResponse = logAndExecCompletions(userInput, systemContext, model);
        logger.info("jsonResponse: " + jsonResponse);
        String contentAsString = GptResponseParser.extractContentAsString(jsonResponse);
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
        };
    }

    @Override
    public <TRequest, TResponse> TResponse executeAndExtractContent(TypedPromptRequest<TRequest, TResponse> request) {
        if (request.getRequest() instanceof String && request.getResponseClass() == String.class) {
            @SuppressWarnings("unchecked")
            TResponse result = (TResponse) executeAndExtractContent(
                    (String) request.getRequest(),
                    request.getSystemContext(),
                    request.getModel()
            );
            return result;
        }

        return executeAndExtractContent(
                request.getRequest(),
                request.getSystemContext(),
                request.getResponseClass(),
                request.getModel()
        );
    }

}
