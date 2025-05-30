package com.avpuser.ai.executor;

import com.avpuser.ai.AIModel;
import com.avpuser.progress.EmptyProgressListener;
import com.avpuser.progress.ProgressListener;
import lombok.Getter;

/**
 * A typed AI request that includes the input object, target response class, and configuration for execution.
 * Used for JSON-based serialization/deserialization through the AI model.
 */
@Getter
public class TypedPromptRequest<TRequest, TResponse> {

    /**
     * The input request object that will be serialized to JSON.
     */
    private final TRequest request;

    /**
     * The system context (system prompt) for the AI model.
     */
    private final String systemContext;

    /**
     * The expected response type to deserialize into.
     */
    private final Class<TResponse> responseClass;

    /**
     * The model used to process the request.
     */
    private final AIModel model;

    /**
     * Optional progress listener for tracking execution state.
     */
    private final ProgressListener progressListener;

    /**
     * Optional label to help identify this request type in cache/logs.
     */
    private final String promptType;

    protected TypedPromptRequest(TRequest request, String systemContext, Class<TResponse> responseClass, AIModel model, ProgressListener progressListener, String promptType) {
        this.request = request;
        this.systemContext = systemContext;
        this.responseClass = responseClass;
        this.model = model;
        this.progressListener = progressListener;
        this.promptType = promptType;
    }

    // --- Static builders to reduce verbosity ---

    public static <TRequest, TResponse> TypedPromptRequest<TRequest, TResponse> of(
            TRequest request,
            String systemContext,
            Class<TResponse> responseClass,
            AIModel model,
            String promptType
    ) {
        return new TypedPromptRequest<>(request, systemContext, responseClass, model, new EmptyProgressListener(), promptType);
    }

    public static <TRequest, TResponse> TypedPromptRequest<TRequest, TResponse> of(
            TRequest request,
            String systemContext,
            Class<TResponse> responseClass,
            AIModel model,
            ProgressListener listener,
            String promptType
    ) {
        return new TypedPromptRequest<>(request, systemContext, responseClass, model, listener, promptType);
    }
}