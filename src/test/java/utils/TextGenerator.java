package utils;

import com.github.pemistahl.lingua.api.Language;

import java.util.Random;

public class TextGenerator {

    private static final String[] RUSSIAN_WORDS = {"привет", "мир", "язык", "русский", "машина", "текст", "пример", "код", "разработка", "анализ", "детектор"};

    private static final String[] ENGLISH_WORDS = {"hello", "world", "language", "russian", "machine", "text", "example", "code", "development", "analysis", "detector"};

    public static String generateLongText(Language language, int targetLength) {
        String[] words;
        if (language.equals(Language.RUSSIAN)) {
            words = RUSSIAN_WORDS;
        } else if (language.equals(Language.ENGLISH)) {
            words = ENGLISH_WORDS;
        } else {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }

        StringBuilder sb = new StringBuilder(targetLength + 1000);
        Random random = new Random();

        while (sb.length() < targetLength) {
            String word = words[random.nextInt(words.length)];
            sb.append(word).append(" ");
        }

        return sb.toString();
    }


}