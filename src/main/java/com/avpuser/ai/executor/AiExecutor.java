package com.avpuser.ai.executor;

public interface AiExecutor {

    /**
     * Executes a typed AI request. If the request/response type is String,
     * it is treated as a raw prompt. Otherwise, it is serialized and deserialized as JSON.
     *
     * @param request A typed prompt request containing all required information
     * @return Response returned by the AI model
     */
    String execute(AiPromptRequest request);

}
