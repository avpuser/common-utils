package ai;

import com.avpuser.ai.executor.AiExecutor;
import com.avpuser.ai.executor.AiPromptRequest;
import com.avpuser.ai.executor.AiWithProgressExecutor;
import com.avpuser.progress.ProgressListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiWithProgressExecutorTest {

    private AiExecutor mockExecutor;
    private ProgressListener mockProgressListener;
    private AiPromptRequest mockRequest;
    private AiWithProgressExecutor progressExecutor;

    @BeforeEach
    void setUp() {
        mockExecutor = mock(AiExecutor.class);
        mockProgressListener = mock(ProgressListener.class);
        mockRequest = mock(AiPromptRequest.class);
        progressExecutor = new AiWithProgressExecutor(mockExecutor);

        when(mockRequest.getProgressListener()).thenReturn(mockProgressListener);
    }

    @Test
    void shouldDelegateExecutionToWrappedExecutor() {
        when(mockRequest.getPromptType()).thenReturn("test_prompt");
        when(mockExecutor.execute(mockRequest)).thenReturn("response");

        String result = progressExecutor.execute(mockRequest);

        assertEquals("response", result);
        verify(mockExecutor, times(1)).execute(mockRequest);
        verify(mockRequest, times(1)).getProgressListener();
    }

    @Test
    void shouldReportProgressDuringExecution() {
        when(mockExecutor.execute(mockRequest)).thenAnswer(invocation -> {
            mockProgressListener.onProgress(0);
            mockProgressListener.onComplete();
            return "done";
        });

        when(mockRequest.getPromptType()).thenReturn("progress_test");

        String result = progressExecutor.execute(mockRequest);

        assertEquals("done", result);
        verify(mockProgressListener).onProgress(0);
        verify(mockProgressListener).onComplete();
    }

    @Test
    void wrapMethodShouldReturnWrappedInstance() {
        AiWithProgressExecutor wrapped = AiWithProgressExecutor.wrap(mockExecutor);
        assertNotNull(wrapped);
        assertTrue(wrapped instanceof AiWithProgressExecutor);
    }
}