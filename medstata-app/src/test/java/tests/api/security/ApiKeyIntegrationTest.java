package tests.api.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "medstata.api.key=test-integration-key"
})
class ApiKeyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testApiEndpoint_WithValidApiKey() throws Exception {
        mockMvc.perform(get("/api/patients")
                .param("userId", "test-user")
                .header("X-API-Key", "test-integration-key"))
                .andExpect(status().isOk());
    }

    @Test
    void testApiEndpoint_WithoutApiKey() throws Exception {
        mockMvc.perform(get("/api/patients")
                .param("userId", "test-user"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.status").value("forbidden"))
                .andExpect(jsonPath("$.message").value("Missing API key"));
    }

    @Test
    void testApiEndpoint_WithInvalidApiKey() throws Exception {
        mockMvc.perform(get("/api/patients")
                .param("userId", "test-user")
                .header("X-API-Key", "invalid-key"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.status").value("forbidden"))
                .andExpect(jsonPath("$.message").value("Invalid API key"));
    }

    @Test
    void testApiEndpoint_WithEmptyApiKey() throws Exception {
        mockMvc.perform(get("/api/lab-tests")
                .param("userId", "test-user")
                .header("X-API-Key", ""))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.status").value("forbidden"))
                .andExpect(jsonPath("$.message").value("Missing API key"));
    }

    @Test
    void testSwaggerEndpoint_NoApiKeyRequired() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void testApiDocsEndpoint_NoApiKeyRequired() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void testHealthEndpoint_NoApiKeyRequired() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isNotFound()); // Endpoint может не существовать, но API key не требуется
    }

    @Test
    void testRootEndpoint_NoApiKeyRequired() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isNotFound()); // Endpoint может не существовать, но API key не требуется
    }

    @Test
    void testLabReportsEndpoint_WithValidApiKey() throws Exception {
        mockMvc.perform(get("/api/lab-reports")
                .param("userId", "test-user")
                .header("X-API-Key", "test-integration-key"))
                .andExpect(status().isOk());
    }

    @Test
    void testLabReportsEndpoint_WithoutApiKey() throws Exception {
        mockMvc.perform(get("/api/lab-reports")
                .param("userId", "test-user"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("forbidden"));
    }

    @Test
    void testLabTestByIdEndpoint_WithValidApiKey() throws Exception {
        mockMvc.perform(get("/api/lab-tests/test-id")
                .header("X-API-Key", "test-integration-key"))
                .andExpect(status().isOk());
    }

    @Test
    void testLabTestByIdEndpoint_WithoutApiKey() throws Exception {
        mockMvc.perform(get("/api/lab-tests/test-id"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("forbidden"));
    }

    @Test
    void testLabReportByIdEndpoint_WithValidApiKey() throws Exception {
        mockMvc.perform(get("/api/lab-reports/test-report-id")
                .header("X-API-Key", "test-integration-key"))
                .andExpect(status().isOk());
    }

    @Test
    void testLabReportByIdEndpoint_WithoutApiKey() throws Exception {
        mockMvc.perform(get("/api/lab-reports/test-report-id"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("forbidden"));
    }
}
