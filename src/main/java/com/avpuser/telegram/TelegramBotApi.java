package com.avpuser.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.Optional;


//https://core.telegram.org/bots/api
public class TelegramBotApi {

    private static final Logger logger = LogManager.getLogger(TelegramBotApi.class);

    //50 MB
    public static final long MAX_TELEGRAM_FILE_SIZE_THRESHOLD = 50_000_000L;

    //https://core.telegram.org/bots/api#sendaudio
    //The thumbnail should be in JPEG format and less than 200 kB in size
    public static final int MAX_TELEGRAM_THUMBNAIL_SIZE = 200_000;

    //https://core.telegram.org/bots/api#sendaudio
    //Audio caption, 0-1024 characters after entities parsing
    public static final int MAX_TELEGRAM_CAPTION_LEN = 1_024;

    //https://core.telegram.org/bots/api#sendmessage
    //Text of the message to be sent, 1-4096 characters after entities parsing
    public static final int MAX_TELEGRAM_TEXT_LENGTH = 4096;

    // Token received from @BotFather
    private final String token;

    private final String sendAudioUrl;

    private final String baseApiUrl;

    public TelegramBotApi(String token) {
        this.token = token;
        this.baseApiUrl = "https://api.telegram.org/bot" + token + "/";
        this.sendAudioUrl = this.baseApiUrl + "sendAudio";

    }

    //https://core.telegram.org/bots/api#sendaudio
    //caption - Audio caption, 0-1024 characters after entities parsing
    public String sendAudioFromFile(String chatId, String audioTitle, String audioPerformer, Duration duration,
                                    String filePath, String caption, Optional<String> thumbnailFile) {
        logger.info("Sending audio to telegram chat: " + chatId + ". File: " + filePath);
        HttpEntity entity = TelegramHttpUtils.buildHttpEntityForSendAudioFromFile(chatId, audioTitle, audioPerformer, duration,
                filePath, caption, thumbnailFile);
        return TelegramHttpUtils.sendAudioAndGetFileId(entity, sendAudioUrl);
    }

    public String sendAudioFromUrl(String chatId, String audioTitle, String audioPerformer, Duration duration,
                                   String fileUrl, String caption, Optional<String> thumbnailFile) {
        logger.info("Sending audio to telegram chat: " + chatId + ". URL: " + fileUrl);
        HttpEntity entity = TelegramHttpUtils.buildHttpEntityForSendAudioFromUrl(chatId, audioTitle, audioPerformer, duration,
                fileUrl, caption, thumbnailFile);
        return TelegramHttpUtils.sendAudioAndGetFileId(entity, sendAudioUrl);
    }

    public String sendAudioFromFileId(String chatId, String fileId, String caption) {
        logger.info("Sending audio to telegram chat: " + chatId + ". FileId: " + fileId);
        HttpEntity entity = TelegramHttpUtils.buildHttpEntityForSendAudioFromFileId(chatId, fileId, caption);
        return TelegramHttpUtils.sendAudioAndGetFileId(entity, sendAudioUrl);
    }

    // https://core.telegram.org/bots/api#editmessagetext
    public void editMessageText(long messageId, String message, String chatId, ParseMode parseMode) {
        logger.info("Editing message in Telegram chat: " + chatId + ", messageId: " + messageId);
        String url = baseApiUrl + "editMessageText";

        message = TelegramUtils.abbreviate(message, MAX_TELEGRAM_TEXT_LENGTH, parseMode);
        message = TelegramMessageSanitizer.sanitizeMessage(message, parseMode);
        HttpEntity entity = TelegramHttpUtils.buildHttpEntityForEditMessage(messageId, message, chatId, parseMode);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);

            post.setEntity(entity);

            // Execute the HTTP request
            try (CloseableHttpResponse response = client.execute(post)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                logger.info("Telegram response: " + responseBody);

                // Parse JSON response
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);

                // Check if the request was successful
                if (!jsonResponse.get("ok").asBoolean()) {

                    JsonNode description = jsonResponse.get("description");
                    if (description != null && description.toString().contains(
                            "Bad Request: message is not modified: specified new message content and reply markup are exactly the same as a current content and reply markup of the message")) {
                        return;
                    }

                    throw new RuntimeException("Failed to edit message: " + jsonResponse);
                }

                logger.info("Message edited successfully: " + messageId);
            }
        } catch (Exception e) {
            logger.error("Error while editing message in Telegram", e);
            throw new RuntimeException(e);
        }
    }


    //https://core.telegram.org/bots/api#sendmessage
    //return message_id
    public long sendMessage(String message, String chatId, ParseMode parseMode) {
        logger.info("Sending message to telegram chat: " + chatId);
        logger.info("Sending message: " + message);
        String url = baseApiUrl + "sendMessage";

        message = TelegramUtils.abbreviate(message, MAX_TELEGRAM_TEXT_LENGTH, parseMode);
        message = TelegramMessageSanitizer.sanitizeMessage(message, parseMode);

        HttpEntity entity = TelegramHttpUtils.buildHttpEntityForSendMessage(message, chatId, parseMode);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setEntity(entity);
            try (CloseableHttpResponse response = client.execute(post)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                logger.info("Telegram response: " + responseBody);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);

                if (!jsonResponse.get("ok").asBoolean()) {
                    throw new RuntimeException("Failed to send message: " + jsonResponse);
                }
                long messageId = jsonResponse.get("result").get("message_id").asLong();
                return messageId;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


}
