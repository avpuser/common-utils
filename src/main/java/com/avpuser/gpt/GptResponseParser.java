package com.avpuser.gpt;

import com.avpuser.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class GptResponseParser {

    private final static Logger logger = LogManager.getLogger(GptResponseParser.class);

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
        return JsonUtils.deserializeJsonToList(extractContentAsString(aiAnswer), clazz);
    }

    public static <T> T extractContentAsObject(String aiAnswer, Class<T> clazz) {
        String content = extractContentAsString(aiAnswer);
        return JsonUtils.deserializeJsonToObject(content, clazz);
    }

}
