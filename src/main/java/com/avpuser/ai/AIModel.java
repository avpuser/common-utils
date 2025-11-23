package com.avpuser.ai;

import java.math.BigDecimal;

/**
 * Enumeration of supported AI models and their associated pricing.
 *
 * 📌 Sources for pricing information:
 * - OpenAI: https://platform.openai.com/docs/pricing
 * - Google Gemini: https://ai.google.dev/pricing
 * - DeepSeek (estimated): https://api-docs.deepseek.com/quick_start/pricing-details-usd
 */
public enum AIModel {

    GPT_4("gpt-4.1", AIProvider.OPENAI, new BigDecimal("2.00"), new BigDecimal("8.00")),
    GPT_4O("gpt-4o", AIProvider.OPENAI, new BigDecimal("2.50"), new BigDecimal("10.00")),
    GPT_4O_MINI("gpt-4o-mini", AIProvider.OPENAI, new BigDecimal("0.15"), new BigDecimal("0.60")),

    DEEPSEEK_CHAT("deepseek-chat", AIProvider.DEEPSEEK, new BigDecimal("0.27"), new BigDecimal("1.10")),
    DEEPSEEK_REASONER("deepseek-reasoner", AIProvider.DEEPSEEK, new BigDecimal("0.55"), new BigDecimal("2.19")),

    GEMINI_PRO("gemini-2.5-pro", AIProvider.GOOGLE, new BigDecimal("1.25"), new BigDecimal("10.00")),
    GEMINI_FLASH("gemini-2.5-flash", AIProvider.GOOGLE, new BigDecimal("0.30"), new BigDecimal("2.50")),
    ;

    private final String modelName;
    private final AIProvider provider;
    private final BigDecimal inputPricePerMillionTokens;
    private final BigDecimal outputPricePerMillionTokens;

    AIModel(String modelName, AIProvider provider, BigDecimal inputPricePerMillionTokens, BigDecimal outputPricePerMillionTokens) {
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

    public BigDecimal getInputPricePerMillionTokens() {
        return inputPricePerMillionTokens;
    }

    public BigDecimal getOutputPricePerMillionTokens() {
        return outputPricePerMillionTokens;
    }
}