package com.avpuser.utils;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

public class LanguageUtils {

    protected static final Logger logger = LogManager.getLogger(LanguageUtils.class);

    private static final LanguageDetector detector = LanguageDetectorBuilder
            .fromAllLanguages()
            .build();

    public static Optional<Language> detectLanguage(String text) {
        try {
            Language lang = detector.detectLanguageOf(text);
            if (lang == Language.UNKNOWN) {
                return Optional.empty();
            }
            return Optional.of(lang);
        } catch (Exception e) {
            logger.error("Error while detecting language", e);
            return Optional.empty();
        }
    }

    public static boolean isRussianText(String text) {
        return detectLanguage(text)
                .map(lang -> lang.equals(Language.RUSSIAN))
                .orElse(false);
    }
}
