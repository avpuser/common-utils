package com.avpuser.ai.executor;

import com.avpuser.ai.AIModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Data
@ToString
@EqualsAndHashCode
public class AiResponse {

    private final String response;

    private final AIModel model;
}
