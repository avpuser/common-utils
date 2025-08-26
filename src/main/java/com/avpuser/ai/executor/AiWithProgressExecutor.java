package com.avpuser.ai.executor;

import com.avpuser.progress.ProgressWrappedExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Decorator for {@link AiExecutor} that enables progress reporting during AI request execution.
 * <p>
 * This executor wraps an underlying {@link AiExecutor} and delegates execution to it,
 * while ensuring that progress updates are properly handled via the {@link com.avpuser.progress.ProgressListener}
 * provided in the {@link AiPromptRequest}.
 * </p>
 *
 * <p><strong>Use case:</strong> When AI requests are long-running or need to provide feedback to UI/logging systems,
 * this executor allows injecting progress monitoring without altering the base execution logic.</p>
 *
 * <p><strong>Behavior:</strong></p>
 * <ul>
 *     <li>Delegates actual execution to the wrapped executor.</li>
 *     <li>Uses {@link ProgressWrappedExecutor} to invoke the execution block with progress tracking.</li>
 *     <li>Logs the prompt type before execution begins.</li>
 * </ul>
 *
 * <p><strong>Example:</strong></p>
 * <pre>{@code
 * AiExecutor baseExecutor = new OpenAiExecutor(...);
 * AiExecutor withProgress = AiWithProgressExecutor.wrap(baseExecutor);
 * String response = withProgress.execute(requestWithProgressListener);
 * }</pre>
 *
 * @see AiExecutor
 * @see AiPromptRequest
 * @see com.avpuser.progress.ProgressListener
 * @see ProgressWrappedExecutor
 */
public class AiWithProgressExecutor implements AiExecutor {

    private static final Logger logger = LogManager.getLogger(AiWithProgressExecutor.class);

    private final AiExecutor delegate;

    public AiWithProgressExecutor(AiExecutor delegate) {
        this.delegate = delegate;
    }

    /**
     * Static factory method to wrap an existing {@link AiExecutor} with progress support.
     *
     * @param executor The underlying AI executor to wrap
     * @return A progress-enabled AI executor
     */
    public static AiWithProgressExecutor wrap(AiExecutor executor) {
        return new AiWithProgressExecutor(executor);
    }

    /**
     * Executes the given {@link AiPromptRequest}, while reporting progress to the associated listener.
     *
     * @param request AI request containing prompts, model, type and listener
     * @return Response string returned by the AI model
     */
    @Override
    public AiResponse execute(AiPromptRequest request) {
        logger.debug("Executing AI request with progress: {}", request.getPromptType());
        return ProgressWrappedExecutor.runWithProgress(() -> delegate.execute(request), request.getProgressListener());
    }
}