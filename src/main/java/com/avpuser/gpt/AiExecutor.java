package com.avpuser.gpt;

import com.avpuser.gpt.deepseek.DeepSeekApi;
import com.avpuser.gpt.deepseek.DeepSeekModel;
import com.avpuser.gpt.openai.OpenAIApi;
import com.avpuser.gpt.openai.OpenAIModel;
import com.avpuser.progress.ProgressListener;
import com.avpuser.utils.JsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
     * @param request          The request object to be serialized and sent.
     * @param systemContext    The optional system context or prompt instructions.
     * @param responseClass    The target class to deserialize the AI response into.
     * @param progressListener Listener to report progress updates.
     * @return Deserialized response of type TResponse.
     */
    public <TRequest, TResponse> TResponse executeAndExtractContent(TRequest request, String systemContext, Class<TResponse> responseClass, ProgressListener progressListener) {
        String userInput = JsonUtils.toJson(request);
        String aiResponse = executeAndExtractContent(userInput, systemContext, progressListener);
        return JsonUtils.deserializeJsonToObject(aiResponse, responseClass);
    }

    /**
     * Sends a raw user input string and system context to the AI provider,
     * executes the completion request in a separate thread, tracks progress in parallel,
     * and returns the extracted textual content from the JSON response.
     *
     * @param userInput        The raw prompt or input string for the AI.
     * @param systemContext    The optional system context or prompt instructions.
     * @param progressListener Listener to report progress updates.
     * @return Extracted content string from the AI's JSON response.
     */
    public String executeAndExtractContent(String userInput, String systemContext, ProgressListener progressListener) {
        if (userInput == null || userInput.isBlank()) {
            throw new IllegalArgumentException("userInput");
        }

        logger.info("userInput: " + userInput);

        progressListener.onProgress(5);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            AtomicBoolean completed = new AtomicBoolean(false);
            AtomicInteger progress = new AtomicInteger(5);

            Future<?> progressFuture = executor.submit(() -> {
                try {
                    while (!completed.get() && progress.get() < 99) {
                        progressListener.onProgress(progress.getAndAdd(1));
                        Thread.sleep(1_000);
                    }
                } catch (InterruptedException ignored) {
                }
            });

            Future<String> responseFuture = executor.submit(() -> {
                return aiProvider == AIProvider.OPENAI ? openAiApi.execCompletions(userInput, systemContext, openAIModel) : deepSeekApi.execCompletions(userInput, systemContext, deepSeekModel);
            });

            try {
                String jsonResponse = responseFuture.get();
                logger.info("jsonResponse: " + jsonResponse);
                completed.set(true);
                progressFuture.cancel(true);
                progressListener.onProgress(99);
                String contentAsString = GptResponseParser.extractContentAsString(jsonResponse);
                logger.info("contentAsString: " + contentAsString);
                return contentAsString;
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Error waiting for AI response", e);
            } finally {
                executor.shutdownNow();
            }
        }
    }

}
