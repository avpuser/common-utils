package ai;

import com.avpuser.ai.AIModel;
import com.avpuser.ai.AIProvider;
import com.avpuser.ai.AiApiException;
import com.avpuser.ai.executor.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * E2E mock test: RetryAiExecutor + CacheAiExecutor integration with fallback and cache semantics.
 */
class RetryCacheIntegrationTest {

    private AiExecutor innerDelegate;          // "real" low-level executor (e.g., DefaultAiExecutor)
    private PromptCacheService cache;          // cache layer
    private DefaultRetryPolicy policy;         // retry policy

    private AiExecutor cacheExecutor;          // CacheAiExecutor(innerDelegate, cache)
    private RetryAiExecutor retryExecutor;     // RetryAiExecutor(cacheExecutor, policy)

    private AiPromptRequest original;          // original request (not used by cache directly)
    private AiPromptRequest stepA;             // step 1 (model A)
    private AiPromptRequest stepB;             // step 2 (model B)

    private final AIModel modelA = AIModel.GPT_4O;
    private final AIModel modelB = AIModel.GPT_4O_MINI;

    @BeforeEach
    void setUp() {
        innerDelegate  = mock(AiExecutor.class);
        cache          = mock(PromptCacheService.class);
        policy         = mock(DefaultRetryPolicy.class);

        cacheExecutor  = new CacheAiExecutor(innerDelegate, cache);
        retryExecutor  = new RetryAiExecutor(cacheExecutor, policy);

        original = mock(AiPromptRequest.class);
        stepA    = mock(AiPromptRequest.class);
        stepB    = mock(AiPromptRequest.class);

        // steps order for both invocations
        when(policy.stepsFor(original)).thenReturn(List.of(stepA, stepB));

        // meta/log fields
        when(stepA.getModel()).thenReturn(modelA);
        when(stepA.getPromptType()).thenReturn("type-A");
        when(stepB.getModel()).thenReturn(modelB);
        when(stepB.getPromptType()).thenReturn("type-B");
    }

    @Test
    void retryThenCacheHitOnFallback() {
        // ---------- First call ----------

        // Cache lookups:
        // stepA -> miss (twice overall across both calls)
        when(cache.findCached(stepA)).thenReturn(Optional.empty(), Optional.empty());
        // stepB -> 1st call miss, 2nd call hit
        AiResponse okB = new AiResponse("ok", modelB);
        when(cache.findCached(stepB)).thenReturn(Optional.empty(), Optional.of(okB));

        // Delegate behavior:
        // stepA -> throws 429 each time
        AiApiException tooMany = new AiApiException(429, "too many requests", AIProvider.OPENAI);
        when(innerDelegate.execute(stepA)).thenThrow(tooMany);
        // stepB -> success only on first call (second call must not reach delegate)
        when(innerDelegate.execute(stepB)).thenReturn(okB);

        // Retry policy: 429 is retryable
        when(policy.isRetryable(tooMany)).thenReturn(true);

        // First execution: should fallback to B and cache the result
        AiResponse res1 = retryExecutor.execute(original);
        assertSame(okB, res1);

        // Verify first call side-effects
        verify(cache).findCached(stepA);          // miss
        verify(innerDelegate).execute(stepA);     // threw 429
        verify(policy).isRetryable(tooMany);

        verify(cache).findCached(stepB);          // miss
        verify(innerDelegate).execute(stepB);     // success
        verify(cache).save(stepB, okB);           // cached under modelB key

        // ---------- Second call with the same original ----------

        AiResponse res2 = retryExecutor.execute(original);
        assertSame(okB, res2);

        // On second call:
        // stepA again: cache miss -> delegate throws 429 -> retryable
        // stepB: cache hit -> delegate not called, save not called
        verify(cache, times(2)).findCached(stepA);
        verify(innerDelegate, times(2)).execute(stepA);   // threw 429 again
        verify(policy, times(2)).isRetryable(tooMany);    // checked twice total

        verify(cache, times(2)).findCached(stepB);        // miss (1st call) + hit (2nd call)
        verify(innerDelegate, times(1)).execute(stepB);   // only first call
        verify(cache, times(1)).save(stepB, okB);         // only first call

        // Optional: loose ordering checks for the first call path
        InOrder inOrder = inOrder(cache, innerDelegate, policy);
        // First call expected order:
        inOrder.verify(cache).findCached(stepA);
        inOrder.verify(innerDelegate).execute(stepA);
        inOrder.verify(policy).isRetryable(tooMany);
        inOrder.verify(cache).findCached(stepB);
        inOrder.verify(innerDelegate).execute(stepB);
        inOrder.verify(cache).save(stepB, okB);

        // Second call expected partial order (until the cache hit on B)
        inOrder.verify(cache).findCached(stepA);
        inOrder.verify(innerDelegate).execute(stepA);
        inOrder.verify(policy).isRetryable(tooMany);
        inOrder.verify(cache).findCached(stepB); // hit; no further calls for B
        inOrder.verifyNoMoreInteractions();
    }
}