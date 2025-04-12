package com.avpuser.gpt.openai;

public enum OpenAIModel {
    // Enum constants with properties
    GPT_4O_MINI("gpt-4o-mini", 0.15, 0.60),
    GPT_4("gpt-4", 30.00, 60.00);

    // Properties
    private final String modelName;
    private final double inputCostPerMillionTokens;
    private final double outputCostPerMillionTokens;

    // Constructor
    OpenAIModel(String modelName, double inputCostPerMillionTokens, double outputCostPerMillionTokens) {
        this.modelName = modelName;
        this.inputCostPerMillionTokens = inputCostPerMillionTokens;
        this.outputCostPerMillionTokens = outputCostPerMillionTokens;
    }

    // Getters
    public String getModelName() {
        return modelName;
    }

    public double getInputCostPerMillionTokens() {
        return inputCostPerMillionTokens;
    }

    public double getOutputCostPerMillionTokens() {
        return outputCostPerMillionTokens;
    }

    // Method to calculate total cost for a given number of input and output tokens
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

    // Example usage

}