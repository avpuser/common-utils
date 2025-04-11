package com.avpuser.telegram;

import org.telegram.telegrambots.meta.api.objects.Update;

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

}
