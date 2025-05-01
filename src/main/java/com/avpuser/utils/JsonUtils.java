package com.avpuser.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class JsonUtils {

    private static final Logger logger = LogManager.getLogger(FileUtils.class);

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);

    public static <T> T deserializeJsonToObject(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructType(clazz));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing JSON: ", e);
        }
    }

    public static <T> List<T> deserializeJsonToList(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing JSON: ", e);
        }
    }

    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing object to JSON: ", e);
        }
    }

    public static List<Map<String, String>> fromJsonList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing JSON list of maps: ", e);
        }
    }

    public static String escapeJson(String value) {
        try {
            // сериализует строку в безопасный JSON-формат (включая кавычки!)
            return objectMapper.writeValueAsString(value).replaceAll("^\"|\"$", ""); // убираем внешние кавычки
        } catch (Exception e) {
            logger.error("Failed to escape JSON value: " + value, e);
            throw new RuntimeException(e);
        }
    }
}
