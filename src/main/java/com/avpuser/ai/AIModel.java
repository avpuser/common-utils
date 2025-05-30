package com.avpuser.ai;

public enum AIModel {
    /**
     * OpenAI GPT-4 model.
     * <p>
     * High-accuracy general-purpose language model by OpenAI.
     * Suitable for complex reasoning, natural conversation, and content generation tasks.
     * </p>
     * <ul>
     *   <li>Provider: OpenAI</li>
     *   <li>Prompt cost: $30.00 per million tokens</li>
     *   <li>Completion cost: $60.00 per million tokens</li>
     * </ul>
     */

    GPT_4("gpt-4", AIProvider.OPENAI, 30.00, 60.00),
    /**
     * OpenAI GPT-4o-mini model.
     * <p>
     * Lightweight and cost-effective variant of GPT-4o with optimized performance for fast, cheap completions.
     * Ideal for chatbots and real-time responses where cost is a concern.
     * </p>
     * <ul>
     *   <li>Provider: OpenAI</li>
     *   <li>Prompt cost: $0.15 per million tokens</li>
     *   <li>Completion cost: $0.60 per million tokens</li>
     * </ul>
     */
    GPT_4O_MINI("gpt-4o-mini", AIProvider.OPENAI, 0.15, 0.60),

    /**
     * DeepSeek Chat model.
     * <p>
     * General-purpose conversational AI model provided by DeepSeek.
     * Balanced between speed and accuracy, suitable for standard Q&A and summarization tasks.
     * </p>
     * <ul>
     *   <li>Provider: DeepSeek</li>
     *   <li>Prompt cost: $0.07 per million tokens</li>
     *   <li>Completion cost: $1.10 per million tokens</li>
     * </ul>
     */
    DEEPSEEK_CHAT("deepseek-chat", AIProvider.DEEPSEEK, 0.07, 1.10),

    /**
     * DeepSeek Reasoner model.
     * <p>
     * Advanced reasoning model by DeepSeek with enhanced logic capabilities.
     * Designed for complex analysis, structured outputs, and tool usage.
     * </p>
     * <ul>
     *   <li>Provider: DeepSeek</li>
     *   <li>Prompt cost: $0.14 per million tokens</li>
     *   <li>Completion cost: $2.19 per million tokens</li>
     * </ul>
     */
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