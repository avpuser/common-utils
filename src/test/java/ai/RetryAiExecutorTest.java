package ai;

import com.avpuser.ai.AIModel;
import com.avpuser.ai.executor.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RetryAiExecutorTest {

    private AiExecutor delegate;
    private DefaultRetryPolicy policy;
    private RetryAiExecutor retryExecutor;

    private AiPromptRequest originalReq;
    private AiPromptRequest stepReq1;
    private AiPromptRequest stepReq2;

    @BeforeEach
    void setUp() {
        delegate = mock(AiExecutor.class);
        policy = mock(DefaultRetryPolicy.class);
        retryExecutor = new RetryAiExecutor(delegate, policy);

        originalReq = mock(AiPromptRequest.class);

        // Шаги (обычно: текущая модель + фолбэк-модель)
        stepReq1 = mock(AiPromptRequest.class);
        stepReq2 = mock(AiPromptRequest.class);

        when(stepReq1.getModel()).thenReturn(AIModel.GPT_4O);
        when(stepReq1.getPromptType()).thenReturn("type-1");

        when(stepReq2.getModel()).thenReturn(AIModel.GPT_4O_MINI);
        when(stepReq2.getPromptType()).thenReturn("type-2");
    }

    @Test
    void shouldSucceedOnFirstStep_NoRetry() {
        when(policy.stepsFor(originalReq)).thenReturn(List.of(stepReq1, stepReq2));
        AiResponse ok = new AiResponse("ok", AIModel.GPT_4O);
        when(delegate.execute(stepReq1)).thenReturn(ok);

        AiResponse result = retryExecutor.execute(originalReq);

        assertSame(ok, result);
        verify(delegate, times(1)).execute(stepReq1);
        verify(delegate, never()).execute(stepReq2);
        verify(policy, never()).isRetryable(any());
        verify(policy, times(1)).stepsFor(originalReq);
    }

    @Test
    void shouldRetryOnRetryableError_ThenSucceedOnFallback() {
        when(policy.stepsFor(originalReq)).thenReturn(List.of(stepReq1, stepReq2));

        RuntimeException retryable = new RuntimeException("429 or 5xx");
        when(delegate.execute(stepReq1)).thenThrow(retryable);
        when(policy.isRetryable(retryable)).thenReturn(true);

        AiResponse ok = new AiResponse("ok-fallback", AIModel.GPT_4O_MINI);
        when(delegate.execute(stepReq2)).thenReturn(ok);

        AiResponse result = retryExecutor.execute(originalReq);

        assertSame(ok, result);
        verify(delegate).execute(stepReq1);
        verify(delegate).execute(stepReq2);
        verify(policy).isRetryable(retryable);
        verify(policy).stepsFor(originalReq);
    }

    @Test
    void shouldThrowImmediatelyOnNonRetryableError() {
        when(policy.stepsFor(originalReq)).thenReturn(List.of(stepReq1, stepReq2));

        RuntimeException nonRetryable = new RuntimeException("400/403/etc");
        when(delegate.execute(stepReq1)).thenThrow(nonRetryable);
        when(policy.isRetryable(nonRetryable)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> retryExecutor.execute(originalReq));

        assertEquals("Non-retryable failure", ex.getMessage());
        assertSame(nonRetryable, ex.getCause());
        verify(delegate, times(1)).execute(stepReq1);
        verify(delegate, never()).execute(stepReq2);
        verify(policy).isRetryable(nonRetryable);
        verify(policy).stepsFor(originalReq);
    }

    @Test
    void shouldThrowAfterAllStepsExhausted_LastErrorAsCause() {
        when(policy.stepsFor(originalReq)).thenReturn(List.of(stepReq1, stepReq2));

        RuntimeException e1 = new RuntimeException("retryable-1");
        RuntimeException e2 = new RuntimeException("retryable-2");

        when(delegate.execute(stepReq1)).thenThrow(e1);
        when(delegate.execute(stepReq2)).thenThrow(e2);

        when(policy.isRetryable(e1)).thenReturn(true);
        when(policy.isRetryable(e2)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> retryExecutor.execute(originalReq));

        assertEquals("All retry steps exhausted", ex.getMessage());
        assertSame(e2, ex.getCause()); // последняя причина — в cause
        verify(delegate).execute(stepReq1);
        verify(delegate).execute(stepReq2);
        verify(policy).isRetryable(e1);
        verify(policy).isRetryable(e2);
        verify(policy).stepsFor(originalReq);
    }

    @Test
    void shouldCallPolicyStepsForWithOriginalRequest() {
        when(policy.stepsFor(originalReq)).thenReturn(List.of(stepReq1));
        when(delegate.execute(stepReq1)).thenReturn(new AiResponse("ok", AIModel.GPT_4O));

        retryExecutor.execute(originalReq);

        verify(policy, times(1)).stepsFor(originalReq);
    }

    // ------- Дополнительные кейсы -------

    @Test
    void shouldThrowWhenStepsListIsEmpty() {
        when(policy.stepsFor(originalReq)).thenReturn(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> retryExecutor.execute(originalReq));

        assertEquals("All retry steps exhausted", ex.getMessage());
        assertNull(ex.getCause());
        verify(policy).stepsFor(originalReq);
        verifyNoInteractions(delegate);
    }

    @Test
    void singleStep_RetryableError_ShouldExhaustAndThrowWithCause() {
        when(policy.stepsFor(originalReq)).thenReturn(List.of(stepReq1));

        RuntimeException e1 = new RuntimeException("transient");
        when(delegate.execute(stepReq1)).thenThrow(e1);
        when(policy.isRetryable(e1)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> retryExecutor.execute(originalReq));

        assertEquals("All retry steps exhausted", ex.getMessage());
        assertSame(e1, ex.getCause());
        verify(delegate).execute(stepReq1);
        verify(policy).isRetryable(e1);
    }

    @Test
    void multipleFallbacks_ShouldSucceedOnThirdStep() {
        AiPromptRequest stepReq3 = mock(AiPromptRequest.class);
        when(stepReq3.getModel()).thenReturn(AIModel.GPT_4O);
        when(stepReq3.getPromptType()).thenReturn("type-3");

        when(policy.stepsFor(originalReq)).thenReturn(List.of(stepReq1, stepReq2, stepReq3));

        RuntimeException e1 = new RuntimeException("retryable-1");
        RuntimeException e2 = new RuntimeException("retryable-2");

        when(delegate.execute(stepReq1)).thenThrow(e1);
        when(delegate.execute(stepReq2)).thenThrow(e2);

        when(policy.isRetryable(e1)).thenReturn(true);
        when(policy.isRetryable(e2)).thenReturn(true);

        AiResponse ok = new AiResponse("ok-third", AIModel.GPT_4O);
        when(delegate.execute(stepReq3)).thenReturn(ok);

        AiResponse res = retryExecutor.execute(originalReq);

        assertSame(ok, res);
        verify(delegate).execute(stepReq1);
        verify(delegate).execute(stepReq2);
        verify(delegate).execute(stepReq3);
        verify(policy).isRetryable(e1);
        verify(policy).isRetryable(e2);
        verify(policy).stepsFor(originalReq);
    }

    @Test
    void shouldStopAfterFirstSuccess_NoFurtherDelegateCalls() {
        AiPromptRequest stepReq3 = mock(AiPromptRequest.class);
        when(stepReq3.getModel()).thenReturn(AIModel.GPT_4O);
        when(stepReq3.getPromptType()).thenReturn("type-3");

        when(policy.stepsFor(originalReq)).thenReturn(List.of(stepReq1, stepReq2, stepReq3));

        AiResponse ok = new AiResponse("ok-first", AIModel.GPT_4O);
        when(delegate.execute(stepReq1)).thenReturn(ok);

        AiResponse res = retryExecutor.execute(originalReq);

        assertSame(ok, res);
        verify(delegate, times(1)).execute(stepReq1);
        verify(delegate, never()).execute(stepReq2);
        verify(delegate, never()).execute(stepReq3);
        verify(policy, never()).isRetryable(any());
    }

    @Test
    void policyIsRetryableThrows_ShouldPropagateAndNotTryNextStep() {
        when(policy.stepsFor(originalReq)).thenReturn(List.of(stepReq1, stepReq2));

        RuntimeException e1 = new RuntimeException("some error");
        when(delegate.execute(stepReq1)).thenThrow(e1);

        IllegalStateException policyFailure = new IllegalStateException("policy failed");
        when(policy.isRetryable(e1)).thenThrow(policyFailure);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> retryExecutor.execute(originalReq));

        assertEquals("Retry policy evaluation failed", ex.getMessage());
        assertSame(policyFailure, ex.getCause());
        verify(delegate).execute(stepReq1);
        verify(delegate, never()).execute(stepReq2);
        verify(policy).isRetryable(e1);
    }

    @Test
    void nullSafeLogging_ModelAndTypeCanBeNull_NoNpe() {
        // специально возвращаем null'ы для проверки форматтера логов
        when(stepReq1.getModel()).thenReturn(null);
        when(stepReq1.getPromptType()).thenReturn(null);

        when(policy.stepsFor(originalReq)).thenReturn(List.of(stepReq1));

        RuntimeException e = new RuntimeException("retryable");
        when(delegate.execute(stepReq1)).thenThrow(e);
        when(policy.isRetryable(e)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> retryExecutor.execute(originalReq));

        assertEquals("All retry steps exhausted", ex.getMessage());
        assertSame(e, ex.getCause());
        verify(policy).stepsFor(originalReq);
        verify(policy).isRetryable(e);
        verify(delegate).execute(stepReq1);
    }

    // ------- Новые кейсы: Throwable/Error -------

    @Test
    void delegateThrowsError_NonRetryable_WrappedAndPropagated() {
        when(policy.stepsFor(originalReq)).thenReturn(List.of(stepReq1, stepReq2));

        AssertionError err = new AssertionError("boom-error");
        when(delegate.execute(stepReq1)).thenThrow(err);
        when(policy.isRetryable(err)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> retryExecutor.execute(originalReq));

        assertEquals("Non-retryable failure", ex.getMessage());
        assertSame(err, ex.getCause());
        verify(delegate, times(1)).execute(stepReq1);
        verify(delegate, never()).execute(stepReq2);
        verify(policy).isRetryable(err);
    }

    @Test
    void delegateThrowsError_RetryableTrue_ThenSucceedsOnFallback() {
        when(policy.stepsFor(originalReq)).thenReturn(List.of(stepReq1, stepReq2));

        AssertionError err = new AssertionError("transient-error");
        when(delegate.execute(stepReq1)).thenThrow(err);
        when(policy.isRetryable(err)).thenReturn(true);

        AiResponse ok = new AiResponse("ok", AIModel.GPT_4O_MINI);
        when(delegate.execute(stepReq2)).thenReturn(ok);

        AiResponse res = retryExecutor.execute(originalReq);

        assertSame(ok, res);
        verify(delegate).execute(stepReq1);
        verify(delegate).execute(stepReq2);
        verify(policy).isRetryable(err);
    }

    @Test
    void lastStepThrowsError_RetryableTrue_AllExhausted_WrappedWithErrorAsCause() {
        when(policy.stepsFor(originalReq)).thenReturn(List.of(stepReq1, stepReq2));

        LinkageError e1 = new LinkageError("retryable-1");
        InternalError e2 = new InternalError("retryable-2");

        when(delegate.execute(stepReq1)).thenThrow(e1);
        when(policy.isRetryable(e1)).thenReturn(true);

        when(delegate.execute(stepReq2)).thenThrow(e2);
        when(policy.isRetryable(e2)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> retryExecutor.execute(originalReq));

        assertEquals("All retry steps exhausted", ex.getMessage());
        assertSame(e2, ex.getCause());
        verify(delegate).execute(stepReq1);
        verify(delegate).execute(stepReq2);
        verify(policy).isRetryable(e1);
        verify(policy).isRetryable(e2);
    }

    // ------- Новые кейсы: "checked-like" исключения (обёрнутые) -------

    @Test
    void runtimeWrappingCheckedException_RetryableTrue_GoesToFallbackAndSucceeds() {
        when(policy.stepsFor(originalReq)).thenReturn(List.of(stepReq1, stepReq2));

        IOException checked = new IOException("io-issue");
        RuntimeException wrapped = new RuntimeException("wrapped-checked", checked);

        when(delegate.execute(stepReq1)).thenThrow(wrapped);
        when(policy.isRetryable(wrapped)).thenReturn(true);

        AiResponse ok = new AiResponse("ok-fallback", AIModel.GPT_4O_MINI);
        when(delegate.execute(stepReq2)).thenReturn(ok);

        AiResponse res = retryExecutor.execute(originalReq);

        assertSame(ok, res);
        verify(delegate).execute(stepReq1);
        verify(delegate).execute(stepReq2);
        verify(policy).isRetryable(wrapped);
    }

    @Test
    void runtimeWrappingCheckedException_NonRetryable_WrappedAndPropagated() {
        when(policy.stepsFor(originalReq)).thenReturn(List.of(stepReq1, stepReq2));

        IOException checked = new IOException("io-fail");
        RuntimeException wrapped = new RuntimeException("wrapped-io", checked);

        when(delegate.execute(stepReq1)).thenThrow(wrapped);
        when(policy.isRetryable(wrapped)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> retryExecutor.execute(originalReq));

        assertEquals("Non-retryable failure", ex.getMessage());
        assertSame(wrapped, ex.getCause());
        verify(delegate).execute(stepReq1);
        verify(delegate, never()).execute(stepReq2);
        verify(policy).isRetryable(wrapped);
    }
}