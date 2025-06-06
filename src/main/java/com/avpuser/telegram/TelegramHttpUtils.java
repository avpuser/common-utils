package com.avpuser.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TelegramHttpUtils {

    private static final Logger logger = LogManager.getLogger(TelegramHttpUtils.class);

    public static HttpEntity buildHttpEntityForEditMessage(long messageId, String message, String chatId, ParseMode parseMode) {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("chat_id", chatId)); // Target chat ID
        params.add(new BasicNameValuePair("message_id", String.valueOf(messageId))); // Message ID to edit
        params.add(new BasicNameValuePair("text", message)); // New text content
        params.add(new BasicNameValuePair("parse_mode", parseMode.name())); // Formatting mode (HTML, Markdown)
        return new UrlEncodedFormEntity(params, StandardCharsets.UTF_8);
    }

    public static HttpEntity buildHttpEntityForSendMessage(String message, String chatId, ParseMode parseMode) {
        return MultipartEntityBuilder.create()
                .addTextBody("chat_id", chatId, ContentType.TEXT_PLAIN)
                .addTextBody("text", message, ContentType.create(ContentType.TEXT_PLAIN.getMimeType(), StandardCharsets.UTF_8))
                .addTextBody("parse_mode", parseMode.name(), ContentType.TEXT_PLAIN).build();
    }

    public static HttpEntity buildHttpEntityForSendAudioFromFile(String chatId, String audioTitle, String audioPerformer, Duration duration,
                                                                 String filePath, String caption, Optional<String> thumbnailFile) {

        ParseMode captionParseMode = ParseMode.HTML;
        caption = TelegramUtils.abbreviate(caption, TelegramBotApi.MAX_TELEGRAM_CAPTION_LEN, captionParseMode);
        caption = TelegramMessageSanitizer.sanitizeMessage(caption, captionParseMode);

        File file = new File(filePath);

        ContentType contentTextType = ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8);

        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        multipartEntityBuilder.addTextBody("chat_id", chatId, contentTextType);
        multipartEntityBuilder.addTextBody("caption", caption, contentTextType);
        multipartEntityBuilder.addTextBody("parse_mode", captionParseMode.name(), contentTextType);
        multipartEntityBuilder.addTextBody("title", audioTitle, contentTextType);
        multipartEntityBuilder.addTextBody("performer", audioPerformer, contentTextType);
        multipartEntityBuilder.addTextBody("duration", String.valueOf(duration.toSeconds()), contentTextType);
        multipartEntityBuilder.addBinaryBody("audio", file, ContentType.create("audio/mpeg"), file.getName());

        if (thumbnailFile.isPresent()) {
            File f = new File(thumbnailFile.get());
            multipartEntityBuilder.addBinaryBody("thumbnail", f, ContentType.create("image/jpeg"), f.getName());
        }

        return multipartEntityBuilder.build();
    }

    public static HttpEntity buildHttpEntityForSendAudioFromUrl(String chatId, String audioTitle, String audioPerformer, Duration duration,
                                                                String fileUrl, String caption, Optional<String> thumbnailFile) {
        ParseMode captionParseMode = ParseMode.HTML;
        caption = TelegramUtils.abbreviate(caption, TelegramBotApi.MAX_TELEGRAM_CAPTION_LEN, captionParseMode);
        caption = TelegramMessageSanitizer.sanitizeMessage(caption, captionParseMode);

        ContentType contentTextType = ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8);

        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        multipartEntityBuilder.addTextBody("chat_id", chatId, contentTextType);
        multipartEntityBuilder.addTextBody("caption", caption, contentTextType);
        multipartEntityBuilder.addTextBody("parse_mode", captionParseMode.name(), contentTextType);
        multipartEntityBuilder.addTextBody("title", audioTitle, contentTextType);
        multipartEntityBuilder.addTextBody("performer", audioPerformer, contentTextType);
        multipartEntityBuilder.addTextBody("duration", String.valueOf(duration.toSeconds()), contentTextType);
        multipartEntityBuilder.addTextBody("audio", fileUrl, contentTextType);

        if (thumbnailFile.isPresent()) {
            File f = new File(thumbnailFile.get());
            multipartEntityBuilder.addBinaryBody("thumbnail", f, ContentType.create("image/jpeg"), f.getName());
        }

        return multipartEntityBuilder.build();
    }

    public static HttpEntity buildHttpEntityForSendAudioFromFileId(String chatId, String fileId, String caption) {
        ParseMode parseMode = ParseMode.HTML;
        caption = TelegramUtils.abbreviate(caption, TelegramBotApi.MAX_TELEGRAM_CAPTION_LEN, parseMode);
        caption = TelegramMessageSanitizer.sanitizeMessage(caption, parseMode);

        ContentType contentTextType = ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8);

        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        multipartEntityBuilder.addTextBody("chat_id", chatId, contentTextType);
        multipartEntityBuilder.addTextBody("caption", caption, contentTextType);
        multipartEntityBuilder.addTextBody("parse_mode", parseMode.name(), contentTextType);
        multipartEntityBuilder.addTextBody("audio", fileId, contentTextType);

        return multipartEntityBuilder.build();
    }

    public static String sendAudioAndGetFileId(HttpEntity entity, String sendAudioUrl) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(sendAudioUrl);
            post.setEntity(entity);
            try (CloseableHttpResponse response = client.execute(post)) {
                String responseString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                logger.info("Response from Telegram: " + responseString);

                // Parse JSON response
                JSONObject jsonResponse = new JSONObject(responseString);
                if (!jsonResponse.getBoolean("ok")) {
                    throw new RuntimeException("Failed to send audio: " + jsonResponse);
                } else {
                    JSONObject result = jsonResponse.getJSONObject("result");
                    String fileId = result.getJSONObject("audio").getString("file_id");
                    logger.info("Received file_id: " + fileId);
                    return fileId;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send audio", e);
        }
    }

    public static void sendSimpleJsonPost(String url, String jsonPayload) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(jsonPayload, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(post)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                logger.info("Telegram API response from {}: {}", url, responseBody);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);

                if (!jsonResponse.get("ok").asBoolean()) {
                    int errorCode = jsonResponse.path("error_code").asInt(-1);
                    String description = jsonResponse.path("description").asText("Unknown error");

                    if (errorCode == 429) {
                        int retryAfter = jsonResponse.path("parameters").path("retry_after").asInt(0);
                        throw new TelegramRateLimitException(description, retryAfter);
                    }

                    throw new RuntimeException("Telegram API error: " + jsonResponse);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            logger.error("Telegram API POST request failed for URL: " + url, e);
            throw new RuntimeException(e);
        }
    }

    public static String getFieldFromTelegramResponse(String url, String fieldName) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity("{}", StandardCharsets.UTF_8)); // пустое тело

            try (CloseableHttpResponse response = client.execute(post)) {
                String responseBody = EntityUtils.toString(response.getEntity());

                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonResponse = mapper.readTree(responseBody);

                if (!jsonResponse.get("ok").asBoolean()) {
                    throw new RuntimeException("Telegram API error: " + jsonResponse);
                }

                return jsonResponse.path("result").path(fieldName).asText("");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get " + fieldName + " from Telegram API", e);
        }
    }

}
