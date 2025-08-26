package com.avpuser.ai.executor;

import com.avpuser.ai.AIModel;
import com.avpuser.progress.EmptyProgressListener;
import com.avpuser.progress.ProgressListener;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Set;

/**
 * Represents a typed AI prompt request used for sending structured input to an AI model
 * along with contextual metadata. This request can be cached, logged, and tracked via progress listeners.
 * <p>
 * Each request consists of:
 * <ul>
 *     <li>A user-defined input prompt ({@code userPrompt})</li>
 *     <li>An optional system prompt ({@code systemPrompt}) that guides the AI's behavior</li>
 *     <li>An {@link AIModel} specifying which AI provider/model to use</li>
 *     <li>An optional {@link ProgressListener} for tracking execution</li>
 *     <li>A {@code promptType} label for logging, analytics, or caching</li>
 * </ul>
 *
 * <p>Use {@code AiPromptRequest.of(...)} static methods to create new instances with sensible defaults.</p>
 *
 * @see AIModel
 * @see ProgressListener
 * @see PromptCacheService
 */
@Getter
@Data
@ToString
@EqualsAndHashCode
public class AiPromptRequest {

    /**
     * The main user-defined prompt to be sent to the AI model.
     */
    private final String userPrompt;

    /**
     * The optional system-level context that shapes the AI's behavior or tone.
     */
    private final String systemPrompt;

    /**
     * The AI model and provider configuration used to execute the request.
     */
    private final AIModel model;

    /**
     * An optional listener that receives updates about request progress.
     * Defaults to {@link EmptyProgressListener} if not provided.
     */
    private final ProgressListener progressListener;

    /**
     * A custom label identifying the type/category of this prompt.
     * Useful for caching, logging, or analytical purposes.
     */
    private final String promptType;

    private final Set<AIModel> fallbackModels;

    private AiPromptRequest(String userPrompt, String systemPrompt, AIModel model, ProgressListener progressListener, String promptType, Set<AIModel> fallbackModels) {
        this.userPrompt = userPrompt;
        this.systemPrompt = systemPrompt;
        this.model = model;
        this.progressListener = progressListener;
        this.promptType = promptType;
        this.fallbackModels = fallbackModels;
    }

    /**
     * Creates a new {@code AiPromptRequest} using an empty {@link ProgressListener}.
     *
     * @param userPrompt   The main input prompt.
     * @param systemPrompt The system prompt/context.
     * @param model        The AI model to use.
     * @param promptType   A string label to categorize the request.
     * @return A new {@link AiPromptRequest} instance.
     */
    public static AiPromptRequest of(String userPrompt, String systemPrompt, AIModel model, String promptType) {
        return new AiPromptRequest(userPrompt, systemPrompt, model, new EmptyProgressListener(), promptType, Set.of());
    }

    public static AiPromptRequest withFallback(String userPrompt, String systemPrompt, AIModel model, String promptType, Set<AIModel> fallbackModels) {
        return new AiPromptRequest(userPrompt, systemPrompt, model, new EmptyProgressListener(), promptType, fallbackModels);
    }

    /**
     * Creates a new {@code AiPromptRequest} with a custom progress listener.
     *
     * @param userPrompt   The main input prompt.
     * @param systemPrompt The system prompt/context.
     * @param model        The AI model to use.
     * @param promptType   A string label to categorize the request.
     * @param listener     A progress listener for tracking.
     * @return A new {@link AiPromptRequest} instance.
     */
    public static AiPromptRequest of(String userPrompt, String systemPrompt, AIModel model, String promptType, ProgressListener listener) {
        return new AiPromptRequest(userPrompt, systemPrompt, model, listener, promptType, Set.of());
    }
}