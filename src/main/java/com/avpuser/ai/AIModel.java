package com.avpuser.ai;

/**
 * Enumeration of supported AI models and their associated pricing.
 *
 * 📌 Sources for pricing information:
 * - OpenAI: https://openai.com/pricing
 * - Google Gemini: https://ai.google.dev/pricing
 * - DeepSeek (estimated): https://deepseek.com or community estimates
 */
public enum AIModel {

    GPT_4("gpt-4", AIProvider.OPENAI, 2.00, 8.00),
    GPT_4O("gpt-4o", AIProvider.OPENAI, 5.00, 20.00),
    GPT_4O_MINI("gpt-4o-mini", AIProvider.OPENAI, 0.60, 2.40),

    DEEPSEEK_CHAT("deepseek-chat", AIProvider.DEEPSEEK, 0.07, 1.10),
    DEEPSEEK_REASONER("deepseek-reasoner", AIProvider.DEEPSEEK, 0.14, 2.19),

    GEMINI_PRO("gemini-2.5-pro", AIProvider.GOOGLE, 1.25, 10.00),
    GEMINI_FLASH("gemini-2.5-flash", AIProvider.GOOGLE, 0.30, 2.50),
    ;

    private final String modelName;
    private final AIProvider provider;
    private final double inputPricePerMillionTokens;
    private final double outputPricePerMillionTokens;

    AIModel(String modelName, AIProvider provider, double inputPricePerMillionTokens, double outputPricePerMillionTokens) {
        this.modelName = modelName;
        this.provider = provider;
        this.inputPricePerMillionTokens = inputPricePerMillionTokens;
        this.outputPricePerMillionTokens = outputPricePerMillionTokens;
    }

    public String getModelName() {
        return modelName;
    }

    public AIProvider getProvider() {
        return provider;
    }

    public double getInputPricePerMillionTokens() {
        return inputPricePerMillionTokens;
    }

    public double getOutputPricePerMillionTokens() {
        return outputPricePerMillionTokens;
    }
}