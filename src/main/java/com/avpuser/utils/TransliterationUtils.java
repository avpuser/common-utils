package com.avpuser.utils;

import java.util.*;

public class TransliterationUtils {

    private static final Map<String, String> CYR_TO_LAT = Map.ofEntries(
            Map.entry("а", "a"), Map.entry("б", "b"), Map.entry("в", "v"),
            Map.entry("г", "g"), Map.entry("д", "d"), Map.entry("е", "e"),
            Map.entry("ё", "yo"), Map.entry("ж", "zh"), Map.entry("з", "z"),
            Map.entry("и", "i"), Map.entry("й", "y"), Map.entry("к", "k"),
            Map.entry("л", "l"), Map.entry("м", "m"), Map.entry("н", "n"),
            Map.entry("о", "o"), Map.entry("п", "p"), Map.entry("р", "r"),
            Map.entry("с", "s"), Map.entry("т", "t"), Map.entry("у", "u"),
            Map.entry("ф", "f"), Map.entry("х", "kh"), Map.entry("ц", "ts"),
            Map.entry("ч", "ch"), Map.entry("ш", "sh"), Map.entry("щ", "shch"),
            Map.entry("ъ", ""), Map.entry("ы", "y"), Map.entry("ь", ""),
            Map.entry("э", "e"), Map.entry("ю", "yu"), Map.entry("я", "ya")
    );

    private static final LinkedHashMap<String, String> LAT_TO_CYR = new LinkedHashMap<>() {{
        put("shch", "щ");
        put("zh", "ж");
        put("kh", "х");
        put("ts", "ц");
        put("ch", "ч");
        put("sh", "ш");
        put("ph", "ф");
        put("th", "т");
        put("ck", "к");
        put("ya", "я");
        put("ia", "я");
        put("yo", "ё");
        put("yu", "ю");
        put("ks", "кс");
        put("x", "кс"); // критично для Alex → Алекс
        put("e", "е");
        put("a", "а");
        put("b", "б");
        put("v", "в");
        put("g", "г");
        put("d", "д");
        put("z", "з");
        put("i", "и");
        put("y", "ы"); // иногда й
        put("j", "й");
        put("k", "к");
        put("l", "л");
        put("m", "м");
        put("n", "н");
        put("o", "о");
        put("p", "п");
        put("r", "р");
        put("s", "с");
        put("t", "т");
        put("u", "у");
        put("f", "ф");
        put("h", "х");
        put("c", "к");
    }};

    public static String transliterateToLatin(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toLowerCase(Locale.ROOT).toCharArray()) {
            sb.append(CYR_TO_LAT.getOrDefault(String.valueOf(c), String.valueOf(c)));
        }
        return sb.toString();
    }

    public static String transliterateToCyrillic(String text) {
        StringBuilder result = new StringBuilder();
        String lower = text.toLowerCase(Locale.ROOT);
        int i = 0;
        while (i < lower.length()) {
            boolean matched = false;
            for (int len = 4; len > 0; len--) {
                if (i + len <= lower.length()) {
                    String sub = lower.substring(i, i + len);
                    if (LAT_TO_CYR.containsKey(sub)) {
                        result.append(LAT_TO_CYR.get(sub));
                        i += len;
                        matched = true;
                        break;
                    }
                }
            }
            if (!matched) {
                result.append(lower.charAt(i));
                i++;
            }
        }
        return result.toString();
    }
}
