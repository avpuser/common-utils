package com.avpuser.telegram;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Optional;

public class TelegramBotUtils {

    public static Optional<Long> getChatId(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            return Optional.of(update.getMessage().getChatId());
        }

        if (update.getCallbackQuery() != null && update.getCallbackQuery().getMessage() != null) {
            return Optional.of(update.getCallbackQuery().getMessage().getChatId());
        }

        return Optional.empty();
    }

    public static Optional<org.telegram.telegrambots.meta.api.objects.User> getTelegramUser(Update update) {
        if (update.getMessage() != null) {
            return Optional.of(update.getMessage().getFrom());
        }

        if (update.getCallbackQuery() != null && update.getCallbackQuery().getMessage() != null) {
            return Optional.of(update.getCallbackQuery().getFrom());
        }

        return Optional.empty();
    }

    public static String getFilePath(TelegramLongPollingBot telegramBot, String fileId) {
        GetFile getFile = new GetFile();
        getFile.setFileId(fileId);
        File file;
        try {
            file = telegramBot.execute(getFile);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        return file.getFilePath();
    }

    public static String getFileUrl(TelegramLongPollingBot telegramBot, String fileId) {
        return getFileUrl(telegramBot.getBotToken(), getFilePath(telegramBot, fileId));
    }

    public static String getFileUrl(String botToken, String filePath) {
        return "https://api.telegram.org/file/bot" + botToken + "/" + filePath;
    }


    public static Optional<Document> getDocumentO(Update update) {
        if (!update.hasMessage()) {
            return Optional.empty();
        }
        Message message = update.getMessage();
        if (message.hasDocument()) {
            return Optional.of(message.getDocument());
        }
        return Optional.empty();
    }

}
