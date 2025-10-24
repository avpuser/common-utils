package com.avpuser.ai.executor;

import com.avpuser.ai.AIModel;
import com.avpuser.ai.AiApiException;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public final class DefaultRetryPolicy {

    public Iterable<AiPromptRequest> stepsFor(AiPromptRequest req) {
        List<AiPromptRequest> models = new ArrayList<>();
        models.add(AiPromptRequest.of(req.getUserPrompt(), req.getSystemPrompt(), req.getModel(), req.getPromptType()));
        if (req.getFallbackModels() != null) {
            for (AIModel m : req.getFallbackModels())
                if (!m.equals(req.getModel())) {
                    models.add(AiPromptRequest.of(req.getUserPrompt(), req.getSystemPrompt(), m, req.getPromptType()));
                }
        }
        return models;
    }

    public boolean isRetryable(Throwable t) {
        return hasCause(t, AiApiException.class)
                || hasCause(t, IOException.class)
                || hasCause(t, TimeoutException.class);
    }

    private static boolean hasCause(Throwable t, Class<? extends Throwable> type) {
        return ExceptionUtils.indexOfType(t, type) != -1;
    }

}