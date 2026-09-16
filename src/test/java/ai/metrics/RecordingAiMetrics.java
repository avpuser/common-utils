package ai.metrics;

import com.avpuser.ai.metrics.AiMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test double that records AI metric events for assertions.
 */
public final class RecordingAiMetrics implements AiMetrics {

    public record RequestEvent(String promptType, String primaryModel, String finalModel, String outcome, String recovered) {
    }

    public record FallbackEvent(String promptType, String fromModel, String toModel, String reason) {
    }

    public record AttemptEvent(String provider, String model, String account, String result, String errorType) {
    }

    public final List<RequestEvent> requests = new ArrayList<>();
    public final List<FallbackEvent> fallbacks = new ArrayList<>();
    public final List<AttemptEvent> attempts = new ArrayList<>();
    public final AtomicInteger timersStarted = new AtomicInteger();
    public final AtomicInteger timersStopped = new AtomicInteger();

    @Override
    public RequestTimer startRequestTimer() {
        timersStarted.incrementAndGet();
        return (promptType, primaryModel, finalModel, outcome, recovered) -> timersStopped.incrementAndGet();
    }

    @Override
    public void recordRequest(String promptType,
                              String primaryModel,
                              String finalModel,
                              String outcome,
                              String recovered) {
        requests.add(new RequestEvent(promptType, primaryModel, finalModel, outcome, recovered));
    }

    @Override
    public void recordModelFallback(String promptType,
                                    String fromModel,
                                    String toModel,
                                    String reason) {
        fallbacks.add(new FallbackEvent(promptType, fromModel, toModel, reason));
    }

    @Override
    public void recordAttempt(String provider,
                              String model,
                              String account,
                              String result,
                              String errorType) {
        attempts.add(new AttemptEvent(provider, model, account, result, errorType));
    }
}
