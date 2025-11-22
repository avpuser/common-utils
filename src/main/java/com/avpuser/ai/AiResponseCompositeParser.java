package com.avpuser.ai;

import com.avpuser.ai.executor.AiResponse;

public class AiResponseCompositeParser {

    public static AiResponse extractAiResponse(AIProvider provider, String rawResponse, AIModel model) {
        if (provider == AIProvider.GOOGLE) {
            return GeminiAiResponseParser.extractAiResponse(rawResponse, model);
        } else {
            return OpenAiCompatibleResponseParser.extractAiResponse(rawResponse, model);
        }
    }
}

