package com.avpuser.ai;

import com.avpuser.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class AiResponseParser {

    private final static Logger logger = LogManager.getLogger(AiResponseParser.class);

    public static String extractContentAsJsonString(String jsonResponse) {
        return JsonUtils.stripJsonCodeBlock(extractContentAsString(jsonResponse));
    }

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

    public static <T> List<T> extractContentAsList(String aiAnswer, Class<T> clazz) {
        String jsonString = extractContentAsJsonString(aiAnswer);
        return JsonUtils.deserializeJsonToList(jsonString, clazz);
    }

    public static <T> T extractContentAsObject(String aiAnswer, Class<T> clazz) {
        String jsonString = extractContentAsJsonString(aiAnswer);
        return JsonUtils.deserializeJsonToObject(jsonString, clazz);
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
