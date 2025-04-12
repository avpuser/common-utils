package com.avpuser.gpt.deepseek;

//https://api-docs.deepseek.com/quick_start/pricing
public enum DeepSeekModel {

    DEEPSEEK_CHAT("deepseek-chat", 0.07, 1.10),
    DEEPSEEK_REASONER("deepseek-reasoner", 0.14, 2.19);

    private final String modelName;
    private final double inputCostPerMillionTokens;
    private final double outputCostPerMillionTokens;

    DeepSeekModel(String modelName, double inputCostPerMillionTokens, double outputCostPerMillionTokens) {
        this.modelName = modelName;
        this.inputCostPerMillionTokens = inputCostPerMillionTokens;
        this.outputCostPerMillionTokens = outputCostPerMillionTokens;
    }

    public String getModelName() {
        return modelName;
    }

    public double getInputCostPerMillionTokens() {
        return inputCostPerMillionTokens;
    }

    public double getOutputCostPerMillionTokens() {
        return outputCostPerMillionTokens;
    }

    public double calculateTotalCost(int inputTokens, int outputTokens) {
        double inputCost = (inputTokens / 1_000_000.0) * inputCostPerMillionTokens;
        double outputCost = (outputTokens / 1_000_000.0) * outputCostPerMillionTokens;
        return inputCost + outputCost;
    }

    @Override
    public String toString() {
        return String.format("Model: %s, Input Cost: $%.2f/M tokens, Output Cost: $%.2f/M tokens",
                modelName, inputCostPerMillionTokens, outputCostPerMillionTokens);
    }
}
