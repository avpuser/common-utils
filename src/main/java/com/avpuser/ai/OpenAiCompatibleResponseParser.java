package com.avpuser.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OpenAiCompatibleResponseParser {

    private final static Logger logger = LogManager.getLogger(OpenAiCompatibleResponseParser.class);

    public static String extractContentAsString(String jsonResponse) {
        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(jsonResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        JsonNode messageNode = rootNode.path("choices").get(0).path("message").path("content");

        String content = messageNode.asText();
        logger.info(content);
        return content;
    }

    public static boolean isResponseCutOff(String jsonResponse) {
        try {
            JsonNode finishReasonNode = new ObjectMapper()
                    .readTree(jsonResponse)
                    .path("choices")
                    .get(0)
                    .path("finish_reason");

            return "length".equals(finishReasonNode.asText());
        } catch (Exception e) {
            logger.warn("Failed to parse finish_reason", e);
            return false;
        }
    }
}
