package com.avpuser.telegram_api;

import com.avpuser.utils.HtmlSanitizer;

public class TelegramMessageSanitizer {

    public static String sanitizeMessage(String message, ParseMode parseMode) {
        if (message == null) {
            return null;
        }
        if (parseMode.equals(ParseMode.HTML)) {
            return HtmlSanitizer.escapeHtml(message);
        } else {
            // TODO: Implement other parse modes
            return message;
        }
    }

}
