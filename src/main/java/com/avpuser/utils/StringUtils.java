package com.avpuser.utils;

import org.apache.commons.text.WordUtils;

import java.util.Set;

public class StringUtils {

    private static final Set<String> STOP_WORDS = Set.of("https://",      // Links
            "http://",       // Links
            "www",           // Links
            ".com",          // Links
            "@",             // Links
            ".ru",           // Links
            ".net",          // Links
            "иностранн",     // Ads
            "агент",         // Ads
            "иноагент",      // Ads
            "получите",      // Ads
            "запишитесь",    // Ads
            "реклама",       // Ads
            "вырос",         // Ads
            "клиент",        // Ads
            "бестселлер",    // Ads
            "#",             // Links
            "уникальн",      // Ads
            "ссылк",         // Ads
            "комментар",     // Ads
            "договор",       // Ads
            "искованн",      // Ads
            "потер",         // Ads
            "USDT",          // Ads
            "BTC",           // Ads
            "ETH",           // Ads
            "TRC20",         // Ads
            "ERC20",         // Ads
            "бонус",         // Bonus
            "скидк",         // Discounts and special offers
            "ооо",           // Mentions of companies
            "инн",           // Mentions of companies
            "активировать",  // Activate offers
            "купон",         // Coupons
            "скидка",        // Discounts
            "специальное предложение", // Special offers
            "предложение ограничено", // Limited offers
            "зарегистрируйтесь", // Call to register
            "подпишитесь",  // Call to subscribe
            "подписывайтесь", // Call to subscribe
            "выгодное предложение", // Profitable offers
            "скидочный код",  // Discount codes
            "партнерская программа", // Affiliate programs
            "не упустите",  // Call to action
            "эксклюзив",  // Exclusive offers
            "выгод",  // Benefits
            "суперцена",  // Special prices
            "подарок",  // Gifts
            "гарантия",  // Guarantees
            "выиграй",  // Calls to participate in giveaways
            "акция",  // Promotions
            "только сегодня",  // Time-limited offers
            "станьте спонсором канала",  // Ads
            "спонсор",  // Ads
            "номер карты",  // Ads
            "организация мероприятий",  // Ads
            "@gmail.com",  // Ads
            "мои соцсети", // Ads
            "поддержка канала", // Ads
            "instagram", // Ads
            "tiktok", // Ads

            "limited offer",  // Limited offers (en.)
            "special offer",  // Special offers (en.)
            "subscribe",  // Call to subscribe (en.)
            "discount",  // Discount (en.)
            "promo code",  // Promo code (en.)
            "best deal",  // Best deal (en.)
            "register now",  // Call to register (en.)
            "exclusive",  // Exclusive offers (en.)
            "win",  // Calls to participate in giveaways (en.)
            "free",  // Free offers (en.)
            "limited time",  // Time-limited offers (en.)
            "gift",  // Gift (en.)
            "deal",  // Deal, offer (en.)
            "now only",  // Special offer "now only" (en.)
            "don't miss out",  // Call to not miss the opportunity (en.)
            "guaranteed",  // Guaranteed (en.)
            "official site",  // Official site (en.)
            "check it out"  // Call to check out (en.)
    );

    public static String abbreviate(String str, int len) {
        if (str == null || str.isBlank()) {
            return "";
        }

        str = str.trim();

        if (str.length() <= len) {
            return str;
        }

        // Выбор многоточия в зависимости от длины
        String appendToEnd;

        if (len >= 4) {
            appendToEnd = "...";
        } else {
            appendToEnd = "…";
        }

        int ellipsisLength = appendToEnd.length();

        // Если даже многоточие не помещается
        if (len < ellipsisLength) {
            return appendToEnd.substring(0, Math.max(0, len));
        }

        int maxLen = len - ellipsisLength;

        String abbreviated = WordUtils.abbreviate(str, maxLen, maxLen, appendToEnd);
        return abbreviated.trim();
    }

    public static boolean containsStopWordOrEmpty(String str) {
        if (str == null || str.isBlank()) {
            return true;
        }
        String strLower = str.toLowerCase();
        for (String stopWord : STOP_WORDS) {
            if (strLower.contains(stopWord)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsLetterOrDigit(String s) {
        if (s == null || s.isBlank()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes unnecessary spaces from the input string:
     * - trims leading and trailing whitespace
     * - replaces all sequences of whitespace characters (including tabs and multiple spaces) with a single space
     *
     * @param input the string to process
     * @return a cleaned-up string with normalized spacing
     */
    public static String normalizeSpaces(String input) {
        if (input == null) {
            return null;
        }
        return input.trim().replaceAll("[\\p{Zs}\\s]+", " ");
    }

    /**
     * Capitalizes the first letter of the input string and lowercases the rest.
     * If the input is null or empty, it is returned as-is.
     *
     * @param input the string to process
     * @return a string with the first letter capitalized and the rest in lowercase
     */
    public static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) return input;

        int firstCodePoint = input.codePointAt(0);
        int firstCharCount = Character.charCount(firstCodePoint);

        String first = new String(Character.toChars(Character.toTitleCase(firstCodePoint)));
        String rest = input.substring(firstCharCount).toLowerCase();

        return first + rest;
    }
}
