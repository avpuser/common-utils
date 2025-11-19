package ai;

import com.avpuser.ai.AIModel;
import com.avpuser.ai.executor.AiExecutor;
import com.avpuser.ai.executor.AiPromptRequest;
import com.avpuser.ai.executor.AiResponse;
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
        when(mockExecutor.execute(mockRequest)).thenReturn(new AiResponse("response", AIModel.GPT_4O));

        AiResponse result = progressExecutor.execute(mockRequest);

        assertEquals("response", result.getResponse());
        verify(mockExecutor, times(1)).execute(mockRequest);
        verify(mockRequest, times(1)).getProgressListener();
    }

    @Test
    void shouldReportProgressDuringExecution() {
        when(mockExecutor.execute(mockRequest)).thenAnswer(invocation -> {
            mockProgressListener.onProgress(0);
            mockProgressListener.onComplete();
            return new AiResponse("done", AIModel.GPT_4O);
        });

        when(mockRequest.getPromptType()).thenReturn("progress_test");

        AiResponse result = progressExecutor.execute(mockRequest);

        assertEquals("done", result.getResponse());
        verify(mockProgressListener).onProgress(0);
        verify(mockProgressListener).onComplete();
    }

    @Test
    void wrapMethodShouldReturnWrappedInstance() {
        AiWithProgressExecutor wrapped = AiWithProgressExecutor.wrap(mockExecutor);
        assertNotNull(wrapped);
        assertInstanceOf(AiWithProgressExecutor.class, wrapped);
    }
}