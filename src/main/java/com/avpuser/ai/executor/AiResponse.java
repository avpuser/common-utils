package com.avpuser.ai.executor;

import com.avpuser.ai.AIModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Data
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class AiResponse {

    private final String response;

    private final AIModel model;

    private final Integer inputTokens;   // prompt_tokens

    private final Integer outputTokens;  // completion_tokens

    // Convenience constructor for backward compatibility
    public AiResponse(String response, AIModel model) {
        this(response, model, null, null);
    }
}
