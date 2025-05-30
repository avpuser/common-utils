package com.avpuser.ai;

public interface AIApi {

    String execCompletions(String userPrompt, String systemPrompt, AIModel model);

    AIProvider aiProvider();
}
