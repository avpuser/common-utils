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
public class AiPromptRequest {

    /**
     * The input request object that will be serialized to JSON.
     */
    private final String userPrompt;

    /**
     * The system context (system prompt) for the AI model.
     */
    private final String systemPrompt;

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

    protected AiPromptRequest(String userPrompt, String systemPrompt, AIModel model, ProgressListener progressListener, String promptType) {
        this.userPrompt = userPrompt;
        this.systemPrompt = systemPrompt;
        this.model = model;
        this.progressListener = progressListener;
        this.promptType = promptType;
    }

    // --- Static builders to reduce verbosity ---
    public static AiPromptRequest of(String userPrompt, String systemPrompt, AIModel model, String promptType) {
        return new AiPromptRequest(userPrompt, systemPrompt, model, new EmptyProgressListener(), promptType);
    }

    public static AiPromptRequest of(String userPrompt, String systemPrompt, AIModel model, String promptType, ProgressListener listener) {
        return new AiPromptRequest(userPrompt, systemPrompt, model, listener, promptType);
    }
}