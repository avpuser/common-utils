package utils;

import com.avpuser.utils.LanguageUtils;
import com.github.pemistahl.lingua.api.Language;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class LanguageUtilsTest {

    @Test
    public void testDetectRussian() {
        String text = "Привет, как дела? Сегодня отличная погода в Москве.";
        Optional<Language> detected = LanguageUtils.detectLanguage(text);
        assertTrue(detected.isPresent());
        assertEquals(Language.RUSSIAN, detected.get());
    }

    @Test
    public void testDetectEnglish() {
        String text = "Hello, how are you? It is a beautiful sunny day in London.";
        Optional<Language> detected = LanguageUtils.detectLanguage(text);
        assertTrue(detected.isPresent());
        assertEquals(Language.ENGLISH, detected.get());
    }

    @Test
    @Ignore
    public void testDetectFrench() {
        String text = "Bonjour, comment ça va? Il fait beau aujourd'hui à Paris.";
        Optional<Language> detected = LanguageUtils.detectLanguage(text);
        assertTrue(detected.isPresent());
        assertEquals(Language.FRENCH, detected.get());
    }

    @Test
    public void testEmptyString() {
        Optional<Language> detected = LanguageUtils.detectLanguage("");
        assertFalse(detected.isPresent());
    }

    @Test
    public void testNullInput() {
        Optional<Language> detected = LanguageUtils.detectLanguage(null);
        assertFalse(detected.isPresent());
    }

    @Test
    public void testIsRussianTextTrue() {
        String text = "Привет, как дела? Сегодня отличная погода.";
        assertTrue(LanguageUtils.isRussianText(text));
    }

    @Test
    public void testIsRussianTextFalseEnglish() {
        String text = "Hello, how are you?";
        assertFalse(LanguageUtils.isRussianText(text));
    }

    @Test
    public void testIsRussianTextFalseFrench() {
        String text = "Bonjour, comment ça va?";
        assertFalse(LanguageUtils.isRussianText(text));
    }

    @Test
    public void testIsRussianTextEmpty() {
        String text = "";
        assertFalse(LanguageUtils.isRussianText(text));
    }

    @Test
    public void testIsRussianTextNull() {
        String text = null;
        assertFalse(LanguageUtils.isRussianText(text));
    }

    @Test
    public void testIsRussianLongText() {
        String text = TextGenerator.generateLongText(Language.RUSSIAN, 1_000_000);
        assertTrue(LanguageUtils.isRussianText(text));
    }

    @Test
    public void testIsEnglishLongText() {
        String text = TextGenerator.generateLongText(Language.ENGLISH, 1_000_000);
        assertFalse(LanguageUtils.isRussianText(text));
    }

}
