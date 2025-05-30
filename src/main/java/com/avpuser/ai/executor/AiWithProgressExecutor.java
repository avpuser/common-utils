package com.avpuser.ai.executor;

import com.avpuser.progress.ProgressWrappedExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A decorator for {@link AiExecutor} that wraps the execution in a progress listener.
 * <p>
 * This class delegates AI prompt execution to an underlying {@link AiExecutor},
 * while ensuring that progress is reported via the {@link com.avpuser.progress.ProgressListener}
 * included in the {@link AiPromptRequest}.
 * </p>
 *
 * <p>Typical use case: enable UI or logging components to receive progress updates
 * during long-running AI operations, without modifying core executor logic.</p>
 *
 * @see AiExecutor
 * @see ProgressWrappedExecutor
 */
public class AiWithProgressExecutor implements AiExecutor {

    private final static Logger logger = LogManager.getLogger(AiWithProgressExecutor.class);

    private final AiExecutor aiExecutor;

    public AiWithProgressExecutor(AiExecutor aiExecutor) {
        this.aiExecutor = aiExecutor;
    }

    public static AiWithProgressExecutor wrap(AiExecutor executor) {
        return new AiWithProgressExecutor(executor);
    }

    @Override
    public String execute(AiPromptRequest request) {
        logger.debug("Executing AI request with progress: {}", request.getPromptType());
        return ProgressWrappedExecutor.runWithProgress(() -> aiExecutor.execute(request), request.getProgressListener());
    }
}
