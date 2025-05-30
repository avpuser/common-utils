import com.avpuser.gpt.executor.AiExecutor;
import com.avpuser.gpt.executor.AiWithProgressExecutor;
import com.avpuser.gpt.executor.TypedPromptRequest;
import com.avpuser.progress.ProgressListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class AiWithProgressExecutorTest {

    private AiExecutor delegateExecutor;
    private AiWithProgressExecutor progressExecutor;

    @BeforeEach
    void setUp() {
        delegateExecutor = mock(AiExecutor.class);
        progressExecutor = new AiWithProgressExecutor(delegateExecutor);
    }

    @Test
    void testExecuteAndExtractContent_WithProgressWrapping() {
        TypedPromptRequest<String, String> request = TypedPromptRequest.of(
                "Hello", "System", String.class, null, mock(ProgressListener.class), "test"
        );

        when(delegateExecutor.executeAndExtractContent(request)).thenReturn("World");

        String result = progressExecutor.executeAndExtractContent(request);

        assertEquals("World", result);
        verify(delegateExecutor).executeAndExtractContent(request);
    }

    @Test
    void testWrapStaticMethodCreatesInstance() {
        AiWithProgressExecutor wrapped = AiWithProgressExecutor.wrap(delegateExecutor);
        assertNotNull(wrapped);
    }
}