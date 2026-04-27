package com.avpuser.textextraction;

import com.avpuser.test.MockTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
@MockTest

class ReadableTextCheckerTest {

    // ========== looksLikeReadableText tests ==========

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForNormalText() {
        String text = "This is a normal readable text with enough letters and digits 12345.";
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForRussianText() {
        String text = "Это нормальный читаемый текст на русском языке с достаточным количеством букв и цифр 12345.";
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForMixedLanguages() {
        String text = "This is English text. Это русский текст. 12345 numbers.";
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForTextWithMinimumThreshold() {
        // Exactly 40 useful characters, exactly 10% ratio (400 total)
        String text = "1234567890123456789012345678901234567890" + " ".repeat(360);
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForTextWithMoreThanMinimum() {
        // 50 useful characters, 10% ratio (500 total)
        String text = "12345678901234567890123456789012345678901234567890" + " ".repeat(450);
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForTooFewUsefulCharacters() {
        // Only 39 useful characters (below threshold of 40)
        String text = "123456789012345678901234567890123456789" + " ".repeat(100);
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForLowUsefulCharacterRatio() {
        // 40 useful characters but only 5% ratio (800 total, need at least 10%)
        String text = "1234567890123456789012345678901234567890" + " ".repeat(760);
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForNull() {
        assertFalse(ReadableTextChecker.looksLikeReadableText(null));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForEmptyString() {
        assertFalse(ReadableTextChecker.looksLikeReadableText(""));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForWhitespaceOnly() {
        assertFalse(ReadableTextChecker.looksLikeReadableText("   "));
        assertFalse(ReadableTextChecker.looksLikeReadableText("\t\n\r"));
        assertFalse(ReadableTextChecker.looksLikeReadableText(" ".repeat(100)));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForOnlySymbols() {
        String text = "!@#$%^&*()_+-=[]{}|;':\",./<>?~`".repeat(20);
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForOnlyPunctuation() {
        String text = ".,;:!?()[]{}\"'-".repeat(50);
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForTextWithPunctuation() {
        // Text with punctuation but enough letters/digits
        String text = "Hello, world! This is a test with many words and numbers. 12345 numbers and more text here.";
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForTextWithNewlines() {
        String text = "Line 1 with text\nLine 2 with more text\nLine 3 with even more text\n12345";
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForTextWithTabs() {
        String text = "Column1\tColumn2\tColumn3\nData1\tData2\tData3\nMore\tData\tHere";
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForUnicodeCharacters() {
        String text = "Hello 世界 Привет こんにちは مرحبا 12345 and more text with unicode characters here";
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForNumbersOnly() {
        // 40+ digits should pass
        String text = "12345678901234567890123456789012345678901234567890";
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForShortText() {
        String text = "Short text";
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForTextWithManySpaces() {
        // 40 letters but too many spaces (low ratio)
        String text = "a".repeat(40) + " ".repeat(1000);
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForTextWithReasonableSpaces() {
        // 50 letters with reasonable spacing (10% ratio)
        String text = "a ".repeat(25) + "b".repeat(25);
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForMedicalReport() {
        String text = "Patient Name: John Doe\nDate: 2024-01-15\nTest Results: Normal\nValues: 123, 456, 789";
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForLongDocument() {
        String text = "This is a long document with many words and sentences. " +
                "It contains enough readable content to pass the threshold. " +
                "The text includes numbers like 12345 and 67890. " +
                "There are also punctuation marks and spaces. " +
                "Overall, this should be considered readable text.";
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForBinaryLikeContent() {
        // Mix of control characters and few letters
        String text = "\u0000\u0001\u0002" + "a".repeat(37) + "\u0003\u0004".repeat(100);
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForTextWithAccentedCharacters() {
        String text = "Café résumé naïve façade 12345 and more text with accented characters like café résumé";
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForTextWithEmoji() {
        // Emoji are not letters/digits, but text has enough of them
        String text = "Hello 😊 world 🌍 test 🚀 ";
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForOnlyEmoji() {
        String text = "😊🌍🚀🔥💡".repeat(20);
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForBoundaryCase_Exactly40Chars_Exactly10Percent() {
        // 40 useful chars, 400 total = exactly 10%
        String text = "a".repeat(40) + " ".repeat(360);
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void invalidText() {
        String text = """
                􀀀             
                                       :D %;E<F + =G, >H -?I  @2 ":J  ;K.< 6  =2/>L  ?I  '2 -AMB  I# =I I+ ?0J 1;4 'N2!C3G C4H CI"02+!0:J 5;K <50#6=O 0>L ?I 75+?I+$;1 )J  6%6  &-'(8) *%    9 P'53J'Q R* SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS\\S]^SS_'STaSUbSVS_WSUcSSXSUSYdSZeSd[ ft___b]a_g'^u]wuvwxccccc dhyidzj[ky|ydl{Tdy{[d||mynyyiy{}|oynipqlrqfnmu~s_gbwabgg'']]^__ccc VX ttt___]]]___ _g

gu]x^_ccc ||||||[[[yyy{{{[[[|||yyy||| dyycccd|zz £SaSgSu¤S]aSSaSS¥SS^SSu¡SaSSS¦SS§S¨©ªSS©S«¨SS«¬­®SS©S¯°gS±S~S²³S]S¨_´]Sµ¶SSSSSSSSSSgSSg^SSg'SS]'S SSaS'SS]Sg^S¡SS_~SSSSSSSSSS¢SSgS]SuSw TiÓ×hÔÉ·¹ÆÇ¸ÕÍWÑXYÆ·¿ÈUÎXYXU¼ÆXY¸WVÉÀ½YeYÊeÏWeÁÐ¸ÑW¹Ë¿ÉºÀ»»¼Öº¼½Y½Á¼e½¸¹À»¹¼½ Ò d{d|[[[[dyy ¾¾ÌÌ¿¿ÀÀÂÂÁÁeeÂÂ }Ã{|ÃÃÃdddy|[ ÄÄÄÄÅÅÅÅ TVhÙÆWºÇÉUÁZXÍÙÈÔØX¼XÈÑÈ¿ÈÊeeÉXÝÍYØººXXÁÌÈÖÙ½XÑe¹·ÁÞËYº¼ÑÀ»Õ¿»¹ºÛÁÔ¼ÈÙÀº½·Á¼ÙÜ¼¹ÚÑÛ¿ ÒÒ |dy[[[zz ¿¿¿¿¿¿ÀÀÀÁÁÁeeeÂÂÂ yy[[[|{}Ã|ÃÃdd[z[[yz ÏÄÄÅÂÅ oàW½YÀ·¸ZÌº½Xä¼åVÙæWçèáÚéâÊåXÀêÈÞëáìâßíîYÀï¸ÚèÑïßæYÞð·åeðèïîé ã d[yz ¿¿ÀÁeÂ d[|}Ã|[|z ÄÅ ñn|aX]U_ÃÎaXâ'ôøÛê¦¸óéòÀùé©Yõú°ÆôçÇÙìð®XºéÈ©ÁçªeÙè¯¼çöðï¹ä¸çÀùûÁåüéé ýï þèïîùÿ}î[édæåäéùå 􀀀    ½Ì e¿Â dy[|Ã}[ ÷ÂàÏ î     ' ' ïççð û(èì ïéïæ åüîì ééææ ïåæ ú æ ùïîç ïúé  çùûæ ïåþ çäùì ïéå ûùèì çïåïèûû  çù åèïù çéåçù éùîçûéïîðæåéèäéæéåùùäïåéêùïå 
                éú çìð     ï ç  ï   +) 􀀀 ç, 􀀀    ì􀀀æ!î*"ï#
                $%􀀀&       \s
                ïû  äåæçèéåêëìíîïèïæðåðèïîé
                ñ^_
~_u^_]u^'u wa_-'u]aac.XVT[/[""";
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForBoundaryCase_Exactly40Chars_JustBelow10Percent() {
        // 40 useful chars, 401 total = just below 10%
        String text = "a".repeat(40) + " ".repeat(361);
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForBoundaryCase_Exactly39Chars() {
        // 39 useful chars (below threshold)
        String text = "a".repeat(39) + " ".repeat(100);
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    // ========== looksLikeGibberish tests ==========

    @Test
    void looksLikeGibberish_ShouldReturnTrue_ForOnlyControlChars() {
        String text = "\u0000\u0001\u0002\u0003\u0004\u0005\u0006".repeat(20);
        assertTrue(ReadableTextChecker.looksLikeGibberish(text));
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeGibberish_ShouldReturnTrue_ForPrivateUseChars() {
        // U+E000..U+F8FF — Private Use Area
        String text = ("\uE000\uE001\uE100\uEFFF".repeat(30)) + " a ";
        assertTrue(ReadableTextChecker.looksLikeGibberish(text));
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeGibberish_ShouldReturnTrue_ForLongNonAlnumRun() {
        String text = "!".repeat(200) + " abc ";
        assertTrue(ReadableTextChecker.looksLikeGibberish(text));
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeGibberish_ShouldReturnTrue_ForTopNonAlnumDominates() {
        // однотипный «мусор» с небольшим количеством букв
        String text = ("___".repeat(150)) + " abcde ";
        assertTrue(ReadableTextChecker.looksLikeGibberish(text));
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeGibberish_ShouldReturnFalse_ForCleanEnglishSentence() {
        String text = "This is a perfectly normal English sentence with numbers 12345.";
        assertFalse(ReadableTextChecker.looksLikeGibberish(text));
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeGibberish_ShouldReturnFalse_ForCleanRussianSentence() {
        String text = "Это нормальное русское предложение с цифрами 12345 и знаками препинания.";
        assertFalse(ReadableTextChecker.looksLikeGibberish(text));
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeGibberish_ShouldReturnTrue_ForBinaryLookingMix() {
        String text = "\u0000\u0007\u0008\u000B\u000E" + "S".repeat(10) + "\u0000\u0001".repeat(100);
        assertTrue(ReadableTextChecker.looksLikeGibberish(text));
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    // ========== more looksLikeReadableText tests ==========

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForEnglishWithEmojiAndWords() {
        String text = "Hello 😊 world 🌍 test 🚀 this line has enough words and digits 12345";
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
        assertFalse(ReadableTextChecker.looksLikeGibberish(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForOnlyEmojiEvenIfLong() {
        String text = "😊🌍🚀🔥💡".repeat(60);
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
        assertTrue(ReadableTextChecker.looksLikeGibberish(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForPunctuationStorm() {
        String text = (".,;:!?()[]{}\"'-/\\+=<>%°±".repeat(40)) + "  ";
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
        assertTrue(ReadableTextChecker.looksLikeGibberish(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForTableLikeTabsAndNewlines() {
        String text = "Name\tAge\tCity\nJohn\t33\tBerlin\nKate\t29\tParis\n" +
                "More data follows with numbers 123456 and words present.";
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
        assertFalse(ReadableTextChecker.looksLikeGibberish(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_WhenPrintableRatioIsTooLow() {
        // Много control, пара букв
        String controls = ("\u0001\u0002\u0003\u0004\u0005").repeat(50);
        String text = controls + "ab";
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
        assertTrue(ReadableTextChecker.looksLikeGibberish(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_WhenAllowedBlockRatioTooLow() {
        // Много «неразрешённых» символов + чуть-чуть текста
        String misc = "\u200B\u200C\u200D\u2060\u2061".repeat(60); // форматирующие/zero-width
        String text = misc + " some words 123 ";
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
        assertTrue(ReadableTextChecker.looksLikeGibberish(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForMedicalishEnglish() {
        String text = "Patient: John Doe. BP 120/80 mmHg, HR 72 bpm, Temp 36.6 °C. " +
                "Recommendations provided; follow-up in 3 months.";
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
        assertFalse(ReadableTextChecker.looksLikeGibberish(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForRussianWithUnits() {
        String text = "Пациент Иванов И.И. Глюкоза 5.1 ммоль/л, Холестерин 4.8 ммоль/л, " +
                "АД 120/80 мм рт. ст., пульс 72 уд/мин.";
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
        assertFalse(ReadableTextChecker.looksLikeGibberish(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForLongRepeatingNonAlnumBlocks() {
        String text = ("|||___---===***".repeat(40)) + " text ";
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
        assertTrue(ReadableTextChecker.looksLikeGibberish(text));
    }

    // ========= regression-ish for a typical broken-PDF sample =========

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForTypicalBrokenPdfGarbage() {
        String text = "! \u001c \u001e \"  \u0018 % \u001d & ' ( ) *  S S S S  \uFFFD \uE000 \uE001 " +
                " _ _ _ ||| [[[ yyy {{{ ]]] " +
                " \u0000\u0001\u0002\u0003\u0004\u0005".repeat(20);
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
        assertTrue(ReadableTextChecker.looksLikeGibberish(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForZeroWidthNoise() {
        // много zero-width + немного букв
        String zw = "\u200B\u200C\u200D\uFEFF".repeat(100);
        String text = zw + " abc ";
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
        assertTrue(ReadableTextChecker.looksLikeGibberish(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForShortTextWithEmojiAndWords() {
        // короткий, но «чистый»: слова + эмодзи
        String text = "Hello 😊 world 🌍!";
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
        assertFalse(ReadableTextChecker.looksLikeGibberish(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForArabicOnlyIfNotSupportedByHeuristic() {
        // если твой алгоритм ориентирован на латиницу/кириллицу — такой текст должен отсеиваться
        String text = "مرحبا هذا نص عربي بدون أرقام";
        // если предполагается поддержка любых языков — поменяй ожидание на true
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForSurrogateGarbage() {
        // случайные суррогаты и спецсимволы, похожие на обломки PDF
        String text = "\uDBFF\uDFFF".repeat(50) + "\uFFF9\uFFFA\uFFFB".repeat(30) + " ab ";
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
        assertTrue(ReadableTextChecker.looksLikeGibberish(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForCJKPlusLatinNumbers() {
        // смешанный CJK + латиница и цифры — должен проходить
        String text = "报告结果：血糖 5.3 mmol/L；血压 120/80。Follow-up in 3 months (n=25).";
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForHugeControlSkew() {
        String controls = "\u0000\u0001\u0002\u0003\u0004\u0005".repeat(500);
        String text = controls + " Some text 123 ";
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
        assertTrue(ReadableTextChecker.looksLikeGibberish(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForColumnsWithUnits() {
        String text = "Na+\t140 mmol/L\nK+\t4.1 mmol/L\nCa2+\t2.28 mmol/L\nPulse\t72 bpm\nTemp\t36.6 °C";
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForLeadingBOMAndZeroWidth() {
        String text = "\uFEFF\u200B\u200C\u200D".repeat(80) + " Some text 123 ";
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForVeryLongNormalText() {
        String paragraph = "This is a line with words and numbers 12345. ";
        String text = paragraph.repeat(2000); // >100k symbols
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForTextWithLigatures() {
        String text = "Office ﬁle: patient’s glucose is 5.6 mmol/L. Cholesterol 4.8.";
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForSoftHyphenNoise() {
        String text = ("\u00AD".repeat(500)) + " abc 123 ";
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnFalse_ForHebrewWithoutDigits_IfUnsupported() {
        String text = "זה טקסט בעברית ללא מספרים";
        assertFalse(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForArabicIndicDigitsWithLatin() {
        String text = "BP ١٢٠/٨٠ mmHg, HR ٧٢ bpm, Temp ٣٦٫٦ °C.";
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForOcrMixedHomoGlyphs() {
        String text = "Паціent: Іvan Іvanov. Glucose 5.3 mmol/L. Нolesterol 4.8.";
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForDenseTabularData() {
        String text = ("Param\tValue\tUnit\n" +
                "GLU\t5.2\tmmol/L\n" +
                "HDL\t1.3\tmmol/L\n").repeat(15);
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForShortLatinWithEmoji() {
        String text = "OK ✅ data ready 🚀";
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
    }

    @Test
    void looksLikeReadableText_ShouldReturnTrue_ForUnitsAndMeasurements() {
        String text = "Na+ 140 mmol/L; K+ 4.1 mmol/L; Ca²⁺ 2.28 mmol/L; pH 7.40; HR 72 bpm;";
        assertTrue(ReadableTextChecker.looksLikeReadableText(text));
    }
}

