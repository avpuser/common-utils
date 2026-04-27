package utils;

import com.avpuser.utils.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StringUtilsTest {

    @Test
    public void abbreviateTest() {
        String message = "Это пример длинного сообщения, которое нужно обрезать до определенной длины.";

        Assertions.assertEquals("Это пример длинного сообщен...", StringUtils.abbreviate(message, 30));
        Assertions.assertEquals("Это пример д...", StringUtils.abbreviate(message, 15));
        Assertions.assertEquals("Это при...", StringUtils.abbreviate(message, 10));
        Assertions.assertEquals(30, StringUtils.abbreviate(message, 30).length());
    }

    @Test
    public void testNullInput() {
        assertEquals("", StringUtils.abbreviate(null, 10));
    }

    @Test
    public void testEmptyString() {
        assertEquals("", StringUtils.abbreviate("", 10));
    }

    @Test
    public void testBlankString() {
        assertEquals("", StringUtils.abbreviate("   ", 10));
    }

    @Test
    public void testStringShorterThanLimit() {
        assertEquals("Hello", StringUtils.abbreviate("Hello", 10));
    }

    @Test
    public void testShortString() {
        String input = "Hello world";
        assertEquals("He…", StringUtils.abbreviate(input, 3));
        assertEquals("H…", StringUtils.abbreviate(input, 2));
        assertEquals("…", StringUtils.abbreviate(input, 1));
        assertEquals("", StringUtils.abbreviate(input, 0));
    }

    @Test
    public void testStringWithBlanks() {
        String input = "    Hello world     ";
        assertEquals("Hello world", StringUtils.abbreviate(input, 12));
        assertEquals("Hello world", StringUtils.abbreviate(input, 11));
        assertEquals("Hello w...", StringUtils.abbreviate(input, 10));
        assertEquals("Hello ...", StringUtils.abbreviate(input, 9));
        assertEquals("Hello...", StringUtils.abbreviate(input, 8));
        assertEquals("He…", StringUtils.abbreviate(input, 3));
        assertEquals("H…", StringUtils.abbreviate(input, 2));
        assertEquals("…", StringUtils.abbreviate(input, 1));
        assertEquals("", StringUtils.abbreviate(input, 0));
    }

    @Test
    public void testStringEqualToLimit() {
        assertEquals("1234567890", StringUtils.abbreviate("1234567890", 10));
    }

    @Test
    public void testStringLongerThanLimit() {
        String input = "This is a very long string that should be abbreviated.";
        String result = StringUtils.abbreviate(input, 10);
        assertEquals("This is...", result);
        assertEquals(10, result.length());
    }

    @Test
    public void testNewlinesInString() {
        String input = "Line1\nLine2\nLine3";
        String result = StringUtils.abbreviate(input, 10);
        assertEquals(10, result.length());
        assertEquals("Line1\nL...", result);
    }

    @Test
    public void testTabsAndNewlines() {
        String input = "Hello\t\nWorld\t\n!";
        String result = StringUtils.abbreviate(input, 5);
        assertTrue(result.length() <= input.length());
    }

    @Test
    public void testAbbreviationAtLimit() {
        String input = "12345678901234567890";
        String result = StringUtils.abbreviate(input, 10);
        assertNotEquals(input, result);
        assertTrue(result.endsWith("..."));
    }

    @Test
    public void testUnicodeCharacters() {
        String input = "Это очень длинная строка с юникодом 🌍🚀🔥";
        String result = StringUtils.abbreviate(input, 10);
        assertTrue(result.endsWith("..."));
    }

    @Test
    public void testStringWithMultipleSpaces() {
        String input = "This    has   multiple     spaces";
        String result = StringUtils.abbreviate(input, 10);
        assertTrue(result.endsWith("..."));
    }

    @Test
    public void testOnlySymbols() {
        String input = "@@@###$$$%%%^^^&&&***";
        String result = StringUtils.abbreviate(input, 5);
        assertEquals("@@...", result);
    }

    @Test
    public void testLongHtmlMessage() {
        String input = getHtmlMessage(5_000);
        String result = StringUtils.abbreviate(input, 3_000);
        assertTrue(result.endsWith("..."));
        assertEquals(3_000, result.length());
    }

    private String getHtmlMessage(int length) {
        StringBuilder sb = new StringBuilder();
        String block = "<b>Hello</b> <i>world</i> <code>test</code>\n"; // 43 chars per block

        while (sb.length() + block.length() <= length) {
            sb.append(block);
        }

        // Если чуть-чуть не хватило — добавим до 4100
        int remaining = length - sb.length();
        if (remaining > 0) {
            sb.append("x".repeat(remaining));
        }

        return sb.toString();
    }

    @Test
    public void testNormalizeSpaces() {
        Assertions.assertEquals("This is a test", StringUtils.normalizeSpaces("   This   is   a   test   "));
        Assertions.assertEquals("Hello world", StringUtils.normalizeSpaces("Hello\t\tworld"));
        Assertions.assertEquals("Line one Line two", StringUtils.normalizeSpaces("Line one\nLine two"));
        Assertions.assertEquals("SingleWord", StringUtils.normalizeSpaces("  SingleWord  "));
        Assertions.assertEquals("", StringUtils.normalizeSpaces(""));
        Assertions.assertNull(StringUtils.normalizeSpaces(null));
        Assertions.assertEquals("A B C", StringUtils.normalizeSpaces("A \t  B   \n C"));
        Assertions.assertEquals("", StringUtils.normalizeSpaces("\t \n \r "));
        Assertions.assertEquals("Привет мир", StringUtils.normalizeSpaces("  Привет     мир  "));
        Assertions.assertEquals("Hello !", StringUtils.normalizeSpaces("  Hello     !  "));
        Assertions.assertEquals("A B", StringUtils.normalizeSpaces("A\u2003B")); // em space
        Assertions.assertEquals("Hello 世界", StringUtils.normalizeSpaces("Hello   \t   世界"));
    }

    @Test
    public void testCapitalizeFirstLetter() {
        Assertions.assertEquals("Hello", StringUtils.capitalizeFirstLetter("hello"));
        Assertions.assertEquals("Test", StringUtils.capitalizeFirstLetter("TEST"));
        Assertions.assertEquals("Java", StringUtils.capitalizeFirstLetter("jAVA"));
        Assertions.assertEquals("X", StringUtils.capitalizeFirstLetter("x"));
        Assertions.assertEquals("", StringUtils.capitalizeFirstLetter(""));
        Assertions.assertNull(StringUtils.capitalizeFirstLetter(null));

        Assertions.assertEquals("1abc", StringUtils.capitalizeFirstLetter("1ABC")); // начинается с цифры
        Assertions.assertEquals("!@#", StringUtils.capitalizeFirstLetter("!@#"));   // только символы
        Assertions.assertEquals("😊smile", StringUtils.capitalizeFirstLetter("😊SMILE")); // emoji
        Assertions.assertEquals("Привет", StringUtils.capitalizeFirstLetter("пРИВЕТ"));  // кириллица
        Assertions.assertEquals("Éclair", StringUtils.capitalizeFirstLetter("éCLAIR"));  // accented
        Assertions.assertEquals("Äbc", StringUtils.capitalizeFirstLetter("äBC"));        // umlaut
        Assertions.assertEquals("中文", StringUtils.capitalizeFirstLetter("中文"));         // иероглифы не меняются
    }
}
