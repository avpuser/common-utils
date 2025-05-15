package com.avpuser.gpt;

import com.avpuser.gpt.deepseek.DeepSeekApi;
import com.avpuser.gpt.deepseek.DeepSeekModel;
import com.avpuser.gpt.openai.OpenAIApi;
import com.avpuser.gpt.openai.OpenAIModel;
import com.avpuser.utils.JsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AiExecutor {

    private final static Logger logger = LogManager.getLogger(AiExecutor.class);

    private final OpenAIApi openAiApi;
    private final DeepSeekApi deepSeekApi;
    private final OpenAIModel openAIModel;
    private final DeepSeekModel deepSeekModel;
    private final AIProvider aiProvider;

    public AiExecutor(OpenAIApi openAiApi, DeepSeekApi deepSeekApi, OpenAIModel openAIModel, DeepSeekModel deepSeekModel, AIProvider aiProvider) {
        this.openAiApi = openAiApi;
        this.deepSeekApi = deepSeekApi;
        this.openAIModel = openAIModel;
        this.deepSeekModel = deepSeekModel;
        this.aiProvider = aiProvider;
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
    public <TRequest, TResponse> TResponse executeAndExtractContent(TRequest request, String systemContext, Class<TResponse> responseClass) {
        String userInput = JsonUtils.toJson(request);
        String aiResponse = JsonUtils.stripJsonCodeBlock(executeAndExtractContent(userInput, systemContext));
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
    public String executeAndExtractContent(String userInput, String systemContext) {
        if (userInput == null || userInput.isBlank()) {
            throw new IllegalArgumentException("userInput");
        }
        logger.info("userInput: " + userInput);
        String jsonResponse = execCompletions(userInput, systemContext);
        logger.info("jsonResponse: " + jsonResponse);
        String contentAsString = GptResponseParser.extractContentAsString(jsonResponse);
        logger.info("contentAsString: " + contentAsString);
        return contentAsString;
    }

    private String execCompletions(String userInput, String systemContext) {
        switch (aiProvider) {
            case OPENAI -> {
                return openAiApi.execCompletions(userInput, systemContext, openAIModel);
            }
            case DEEPSEEK -> {
                return deepSeekApi.execCompletions(userInput, systemContext, deepSeekModel);
            }
            default -> throw new IllegalStateException("Unexpected value: " + aiProvider);
        }
    }
}
