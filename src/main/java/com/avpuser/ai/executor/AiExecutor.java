package com.avpuser.ai.executor;

public interface AiExecutor {

    /**
     * Executes a typed AI request. If the request/response type is String,
     * it is treated as a raw prompt. Otherwise, it is serialized and deserialized as JSON.
     *
     * @param request     A typed prompt request containing all required information
     * @param <TRequest>  Type of the request object (can be String or any DTO)
     * @param <TResponse> Type of the expected response (can be String or any DTO)
     * @return Response returned by the AI model
     */
    <TRequest, TResponse> TResponse executeAndExtractContent(
            TypedPromptRequest<TRequest, TResponse> request
    );

}
