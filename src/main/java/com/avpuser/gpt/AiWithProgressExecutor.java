package com.avpuser.gpt;

import com.avpuser.progress.ProgressListener;
import com.avpuser.progress.ProgressWrappedExecutor;

public class AiWithProgressExecutor {

    private final AiExecutor aiExecutor;

    public AiWithProgressExecutor(AiExecutor aiExecutor) {
        this.aiExecutor = aiExecutor;
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
        return ProgressWrappedExecutor.runWithProgress(() -> aiExecutor.executeAndExtractContent(request, systemContext, responseClass), progressListener);
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
        return ProgressWrappedExecutor.runWithProgress(() -> aiExecutor.executeAndExtractContent(userInput, systemContext), progressListener);
    }
}
