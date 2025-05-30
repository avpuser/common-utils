package com.avpuser.gpt.executor;

import com.avpuser.gpt.AIModel;
import com.avpuser.progress.ProgressListener;

/**
 * A unified AI request object used for caching and execution.
 * Intended to be used with CacheManager and AiExecutor.
 */
public class StringPromptRequest extends TypedPromptRequest<String, String> {

    public StringPromptRequest(String request, String systemContext, AIModel model, ProgressListener progressListener, String promptType) {
        super(request, systemContext, String.class, model, progressListener, promptType);
    }
}