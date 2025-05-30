package com.avpuser.mongo.promptcache;

import com.avpuser.ai.AIModel;
import com.avpuser.ai.executor.StringPromptRequest;
import org.apache.commons.codec.digest.DigestUtils;

public class PromptCacheKeyUtils {

    public static String buildHashKey(String promptType, String request, AIModel model) {
        String base = String.join("::", promptType, model.getModelName(), request);
        return DigestUtils.sha256Hex(base);
    }

    public static String buildHashKey(StringPromptRequest request) {
        return buildHashKey(request.getPromptType(), request.getRequest(), request.getModel());
    }

}
