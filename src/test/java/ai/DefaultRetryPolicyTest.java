package ai;

import com.avpuser.ai.AIModel;
import com.avpuser.ai.AIProvider;
import com.avpuser.ai.AiApiException;
import com.avpuser.ai.executor.AiPromptRequest;
import com.avpuser.ai.executor.DefaultRetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Тесты для DefaultRetryPolicy: stepsFor(...) и isRetryable(...),
 * с учётом сигнатуры AiApiException(int, String, AIProvider).
 */
class DefaultRetryPolicyTest {

    private DefaultRetryPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new DefaultRetryPolicy();
    }

    @Test
    void stepsFor_ShouldIncludePrimaryAndUniqueFallbacks_InOrder() {
        // given
        AiPromptRequest original = mock(AiPromptRequest.class);
        when(original.getUserPrompt()).thenReturn("u");
        when(original.getSystemPrompt()).thenReturn("s");
        when(original.getPromptType()).thenReturn("pt");
        when(original.getModel()).thenReturn(AIModel.GPT_4O);
        when(original.getFallbackModels()).thenReturn(Set.of(
                AIModel.GPT_4O,       // дубликат исходной
                AIModel.GPT_4O_MINI  // валидный фолбэк
        ));

        // when
        Iterable<AiPromptRequest> steps = policy.stepsFor(original);

        // then
        Iterator<AiPromptRequest> it = steps.iterator();
        assertTrue(it.hasNext());
        AiPromptRequest step1 = it.next();
        assertNotSame(original, step1); // создаётся новый запрос
        assertEquals("u", step1.getUserPrompt());
        assertEquals("s", step1.getSystemPrompt());
        assertEquals("pt", step1.getPromptType());
        assertEquals(AIModel.GPT_4O, step1.getModel());

        assertTrue(it.hasNext());
        AiPromptRequest step2 = it.next();
        assertEquals("u", step2.getUserPrompt());
        assertEquals("s", step2.getSystemPrompt());
        assertEquals("pt", step2.getPromptType());
        assertEquals(AIModel.GPT_4O_MINI, step2.getModel());

        assertFalse(it.hasNext(), "Должно быть только 2 шага (исходная + уникальный фолбэк)");
    }

    @Test
    void stepsFor_ShouldHandleNullFallbacks() {
        AiPromptRequest original = mock(AiPromptRequest.class);
        when(original.getUserPrompt()).thenReturn("u");
        when(original.getSystemPrompt()).thenReturn("s");
        when(original.getPromptType()).thenReturn("pt");
        when(original.getModel()).thenReturn(AIModel.GPT_4O);
        when(original.getFallbackModels()).thenReturn(null);

        Iterable<AiPromptRequest> steps = policy.stepsFor(original);

        Iterator<AiPromptRequest> it = steps.iterator();
        assertTrue(it.hasNext());
        AiPromptRequest only = it.next();
        assertEquals(AIModel.GPT_4O, only.getModel());
        assertFalse(it.hasNext());
    }

    @Test
    void stepsFor_ShouldHandleEmptyFallbacks() {
        AiPromptRequest original = mock(AiPromptRequest.class);
        when(original.getUserPrompt()).thenReturn("u");
        when(original.getSystemPrompt()).thenReturn("s");
        when(original.getPromptType()).thenReturn("pt");
        when(original.getModel()).thenReturn(AIModel.GPT_4O);
        when(original.getFallbackModels()).thenReturn(Set.of());

        Iterable<AiPromptRequest> steps = policy.stepsFor(original);

        Iterator<AiPromptRequest> it = steps.iterator();
        assertTrue(it.hasNext());
        AiPromptRequest only = it.next();
        assertEquals(AIModel.GPT_4O, only.getModel());
        assertFalse(it.hasNext());
    }

    @Test
    void stepsFor_ShouldIgnoreFallbackEqualToOriginalOnly() {
        AiPromptRequest original = mock(AiPromptRequest.class);
        when(original.getUserPrompt()).thenReturn("u");
        when(original.getSystemPrompt()).thenReturn("s");
        when(original.getPromptType()).thenReturn("pt");
        when(original.getModel()).thenReturn(AIModel.GPT_4O);
        when(original.getFallbackModels()).thenReturn(Set.of(AIModel.GPT_4O));

        Iterable<AiPromptRequest> steps = policy.stepsFor(original);

        Iterator<AiPromptRequest> it = steps.iterator();
        assertTrue(it.hasNext());
        AiPromptRequest only = it.next();
        assertEquals(AIModel.GPT_4O, only.getModel());
        assertFalse(it.hasNext());
    }

    @Test
    void stepsFor_ShouldCopyFieldsToNewRequests() {
        AiPromptRequest original = mock(AiPromptRequest.class);
        when(original.getUserPrompt()).thenReturn("user-xyz");
        when(original.getSystemPrompt()).thenReturn("sys-abc");
        when(original.getPromptType()).thenReturn("pt-123");
        when(original.getModel()).thenReturn(AIModel.GPT_4O);
        when(original.getFallbackModels()).thenReturn(Set.of(AIModel.GPT_4O_MINI));

        Iterable<AiPromptRequest> steps = policy.stepsFor(original);
        Iterator<AiPromptRequest> it = steps.iterator();

        AiPromptRequest step1 = it.next();
        AiPromptRequest step2 = it.next();

        assertEquals("user-xyz", step1.getUserPrompt());
        assertEquals("sys-abc", step1.getSystemPrompt());
        assertEquals("pt-123", step1.getPromptType());
        assertEquals(AIModel.GPT_4O, step1.getModel());

        assertEquals("user-xyz", step2.getUserPrompt());
        assertEquals("sys-abc", step2.getSystemPrompt());
        assertEquals("pt-123", step2.getPromptType());
        assertEquals(AIModel.GPT_4O_MINI, step2.getModel());
    }

    // ---------- isRetryable(...) ----------

    @Test
    void isRetryable_ShouldReturnTrue_For429() {
        AiApiException e = new AiApiException(429, "too many", AIProvider.OPENAI);
        assertTrue(policy.isRetryable(e));
    }

    @Test
    void isRetryable_ShouldReturnTrue_For5xx() {
        assertTrue(policy.isRetryable(new AiApiException(500, "server err", AIProvider.OPENAI)));
        assertTrue(policy.isRetryable(new AiApiException(503, "unavailable", AIProvider.DEEPSEEK)));
    }

    @Test
    void isRetryable_ShouldReturnFalse_For4xxNon429() {
        assertFalse(policy.isRetryable(new AiApiException(400, "bad request", AIProvider.OPENAI)));
        assertFalse(policy.isRetryable(new AiApiException(403, "forbidden", AIProvider.OPENAI)));
        assertFalse(policy.isRetryable(new AiApiException(404, "not found", AIProvider.DEEPSEEK)));
    }

    @Test
    void isRetryable_ShouldReturnFalse_ForNonHttpRanges() {
        assertFalse(policy.isRetryable(new AiApiException(499, "weird", AIProvider.OPENAI)));
        assertFalse(policy.isRetryable(new AiApiException(600, "out of range", AIProvider.OPENAI)));
    }

    @Test
    void isRetryable_ShouldReturnTrue_ForIOExceptionAndTimeout() {
        assertTrue(policy.isRetryable(new IOException("io")));
        assertTrue(policy.isRetryable(new TimeoutException("timeout")));
    }

    @Test
    void isRetryable_ShouldReturnFalse_ForOtherRuntime() {
        assertFalse(policy.isRetryable(new IllegalArgumentException("oops")));
        assertFalse(policy.isRetryable(new RuntimeException("boom")));
    }
}