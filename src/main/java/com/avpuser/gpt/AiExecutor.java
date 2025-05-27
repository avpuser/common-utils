package com.avpuser.gpt;

import com.avpuser.gpt.deepseek.DeepSeekApi;
import com.avpuser.gpt.openai.OpenAIApi;
import com.avpuser.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AiExecutor {

    private final static Logger logger = LogManager.getLogger(AiExecutor.class);

    private final OpenAIApi openAiApi;
    private final DeepSeekApi deepSeekApi;

    public AiExecutor(OpenAIApi openAiApi, DeepSeekApi deepSeekApi) {
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
    public <TRequest, TResponse> TResponse executeAndExtractContent(TRequest request, String systemContext, Class<TResponse> responseClass, AIModel model) {
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
    public String executeAndExtractContent(String userInput, String systemContext, AIModel model) {
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

}
