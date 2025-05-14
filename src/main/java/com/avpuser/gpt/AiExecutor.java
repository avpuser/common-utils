package com.avpuser.gpt;

import com.avpuser.gpt.deepseek.DeepSeekApi;
import com.avpuser.gpt.deepseek.DeepSeekModel;
import com.avpuser.gpt.openai.OpenAIApi;
import com.avpuser.gpt.openai.OpenAIModel;
import com.avpuser.progress.ProgressListener;
import com.avpuser.utils.JsonUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AiExecutor {

    private final OpenAIApi openAiApi;
    private final DeepSeekApi deepSeekApi;
    private final OpenAIModel openAIModel;
    private final DeepSeekModel deepSeekModel;
    private final AIProvider aiProvider;

    public AiExecutor(OpenAIApi openAiApi,
                      DeepSeekApi deepSeekApi,
                      OpenAIModel openAIModel,
                      DeepSeekModel deepSeekModel,
                      AIProvider aiProvider) {
        this.openAiApi = openAiApi;
        this.deepSeekApi = deepSeekApi;
        this.openAIModel = openAIModel;
        this.deepSeekModel = deepSeekModel;
        this.aiProvider = aiProvider;
    }

    public <TRequest, TResponse> TResponse execCompletions(TRequest request, String systemContext, Class<TResponse> responseClass, ProgressListener progressListener) {
        String userInput = JsonUtils.toJson(request);
        String aiResponse = execCompletions(userInput, systemContext, progressListener);
        return JsonUtils.deserializeJsonToObject(aiResponse, responseClass);
    }

    public String execCompletions(String userInput, String systemContext, ProgressListener progressListener) {
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
                return aiProvider == AIProvider.OPENAI
                        ? openAiApi.execCompletions(userInput, systemContext, openAIModel)
                        : deepSeekApi.execCompletions(userInput, systemContext, deepSeekModel);
            });

            try {
                String response = responseFuture.get();
                completed.set(true);
                progressFuture.cancel(true);
                progressListener.onProgress(99);
                return response;
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Error waiting for AI response", e);
            } finally {
                executor.shutdownNow();
            }
        }
    }

}
