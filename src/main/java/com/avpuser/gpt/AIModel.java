package com.avpuser.gpt;

public enum AIModel {
    GPT_4("gpt-4", AIProvider.OPENAI, 30.00, 60.00),
    GPT_4O_MINI("gpt-4o-mini", AIProvider.OPENAI, 0.15, 0.60),
    DEEPSEEK_CHAT("deepseek-chat", AIProvider.DEEPSEEK, 0.07, 1.10),
    DEEPSEEK_REASONER("deepseek-reasoner", AIProvider.DEEPSEEK, 0.14, 2.19);

    private final String modelName;
    private final AIProvider provider;
    private final double pricePrompt;
    private final double priceCompletion;

    AIModel(String modelName, AIProvider provider, double pricePrompt, double priceCompletion) {
        this.modelName = modelName;
        this.provider = provider;
        this.pricePrompt = pricePrompt;
        this.priceCompletion = priceCompletion;
    }

    public String getModelName() {
        return modelName;
    }

    public AIProvider getProvider() {
        return provider;
    }

    public double getPricePrompt() {
        return pricePrompt;
    }

    public double getPriceCompletion() {
        return priceCompletion;
    }
}