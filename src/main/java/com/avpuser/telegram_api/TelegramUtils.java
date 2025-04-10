package com.avpuser.telegram_api;

import com.avpuser.utils.HtmlSanitizer;
import com.avpuser.utils.StringUtils;

public class TelegramUtils {

    public static boolean isFileTooLargeForTelegram(long fileSize) {
        return fileSize > TelegramBotApi.MAX_TELEGRAM_FILE_SIZE_THRESHOLD;
    }

    public static String abbreviate(String str, int len, ParseMode parseMode) {
        if (str == null || str.isBlank()) {
            return "";
        }

        str = str.trim();

        if (str.length() <= len) {
            return str;
        }
        if (parseMode.equals(ParseMode.HTML)) {
            String plainText = HtmlSanitizer.stripHtml(str);
            if (plainText.length() <= len) {
                return str;
            }
        }
        return StringUtils.abbreviate(str, len);
    }

}
