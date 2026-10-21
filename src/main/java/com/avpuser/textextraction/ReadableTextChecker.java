package com.avpuser.textextraction;

import com.avpuser.utils.LanguageUtils;
import com.github.pemistahl.lingua.api.Language;
import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ReadableTextChecker {

    private static final int    MIN_USEFUL_FOR_READABLE = 10;
    private static final double MIN_USEFUL_RATIO        = 0.10;
    private static final int    MIN_TOTAL_LEN           = 12;

    private static final int    MIN_USEFUL_FOR_OK       = 10;
    private static final double MIN_USEFUL_RATIO_OK     = 0.20;
    private static final int    MAX_NONPRINTABLE_FOR_OK = 10;
    private static final double MAX_CONTROL_PCT         = 0.05;
    private static final double MIN_PRINTABLE_RATIO     = 0.70;
    private static final double MIN_ALLOWED_BLOCK_RATIO = 0.60;
    private static final int    MAX_RUN_LENGTH          = 12;
    private static final double MAX_TOP_CHAR_SHARE      = 0.30;

    /** Minimum letters for a short page fragment that is not digit-/symbol-only noise. */
    private static final int    MIN_LETTERS_FOR_SPARSE  = 3;

    public static boolean looksLikeReadableText(String text) {
        if (StringUtils.isBlank(text)) return false;

        Stats s = analyze(text);
        if (s.total < MIN_TOTAL_LEN) return false;
        if (looksLikeGibberishByStats(s)) return false;

        Optional<Language> lang = LanguageUtils.detectLanguage(text);
        return lang.isPresent()
                && s.useful >= MIN_USEFUL_FOR_READABLE
                && s.usefulRatio() >= MIN_USEFUL_RATIO;
    }

    /**
     * Short but clean text: not enough for reliable language detection, yet not gibberish.
     * Accepts sparse PDF pages (headers, footers, a few medical abbreviations) while rejecting
     * digit-only timestamps and symbol noise that often come from broken text layers.
     */
    public static boolean looksLikeCleanSparseText(String text) {
        if (StringUtils.isBlank(text)) return false;

        Stats s = analyze(text);
        if (s.total == 0) return false;
        if (s.letters < MIN_LETTERS_FOR_SPARSE) return false;
        if (s.usefulRatio() < MIN_USEFUL_RATIO_OK) return false;
        // Soft noise checks only — do not require full MIN_USEFUL_FOR_OK / language detection.
        if (s.nonPrintable > MAX_NONPRINTABLE_FOR_OK) return false;
        if (s.controlRatio() > MAX_CONTROL_PCT) return false;
        if (s.printableRatio() < MIN_PRINTABLE_RATIO) return false;
        if (s.allowedBlockRatio() < MIN_ALLOWED_BLOCK_RATIO) return false;
        if (s.maxRun >= MAX_RUN_LENGTH) return false;
        // topCharShare is unreliable on very short strings (e.g. "Short text" has repeated 't').
        if (s.total >= MIN_TOTAL_LEN && s.topCharShare() > MAX_TOP_CHAR_SHARE) return false;
        return true;
    }

    public static boolean looksLikeGibberish(String text) {
        if (StringUtils.isBlank(text)) return true;
        Stats s = analyze(text);
        if (looksLikeGibberishByStats(s)) return true;

        Optional<Language> lang = LanguageUtils.detectLanguage(text);
        return lang.isEmpty();
    }

    // ---------- helpers ----------

    private static boolean looksLikeGibberishByStats(Stats s) {
        if (s.useful < MIN_USEFUL_FOR_OK) return true;
        if (s.usefulRatio() < MIN_USEFUL_RATIO_OK) return true;
        if (s.nonPrintable > MAX_NONPRINTABLE_FOR_OK) return true;
        if (s.controlRatio() > MAX_CONTROL_PCT) return true;
        if (s.printableRatio() < MIN_PRINTABLE_RATIO) return true;
        if (s.allowedBlockRatio() < MIN_ALLOWED_BLOCK_RATIO) return true;
        if (s.maxRun >= MAX_RUN_LENGTH) return true;
        return s.topCharShare() > MAX_TOP_CHAR_SHARE;
    }

    private static Stats analyze(String raw) {
        String s = Normalizer.normalize(raw, Normalizer.Form.NFKC)
                .replaceAll("\\s+", " ")
                .trim();

        Stats st = new Stats();
        Map<Integer,Integer> freq = new HashMap<>();

        int prev = -1;
        int run = 0;

        int[] cps = s.codePoints().toArray();
        for (int cp : cps) {
            if (Character.isWhitespace(cp)) continue;
            st.total++;

            // max run
            if (cp == prev) {
                run++;
            } else {
                run = 1;
                prev = cp;
            }
            if (run > st.maxRun) st.maxRun = run;

            // freq
            freq.merge(cp, 1, Integer::sum);

            // printable vs control
            if (!Character.isISOControl(cp)) st.printable++;

            // letters/digits
            if (Character.isLetterOrDigit(cp)) st.useful++;
            if (Character.isLetter(cp)) st.letters++;

            // noise
            if (cp == 0xFFFD) {
                st.nonPrintable++;
            } else {
                Character.UnicodeBlock b = Character.UnicodeBlock.of(cp);
                if (b == Character.UnicodeBlock.SPECIALS
                        || b == Character.UnicodeBlock.PRIVATE_USE_AREA) {
                    st.nonPrintable++;
                }
            }
            if (Character.isISOControl(cp)) st.controls++;

            // allowed blocks
            if (isAllowedBlock(cp)) st.allowedBlock++;
        }

        st.maxFreq = freq.values().stream().mapToInt(i -> i).max().orElse(0);
        return st;
    }

    private static boolean isAllowedBlock(int cp) {
        Character.UnicodeBlock b = Character.UnicodeBlock.of(cp);
        if (b == null) return false;
        return b == Character.UnicodeBlock.BASIC_LATIN
                || b == Character.UnicodeBlock.LATIN_1_SUPPLEMENT
                || b == Character.UnicodeBlock.LATIN_EXTENDED_A
                || b == Character.UnicodeBlock.LATIN_EXTENDED_B
                || b == Character.UnicodeBlock.CYRILLIC
                || b == Character.UnicodeBlock.CYRILLIC_SUPPLEMENTARY
                || b == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || b == Character.UnicodeBlock.MATHEMATICAL_OPERATORS
                || b == Character.UnicodeBlock.SUPERSCRIPTS_AND_SUBSCRIPTS
                || b == Character.UnicodeBlock.NUMBER_FORMS;
    }

    private static final class Stats {
        int total;
        int useful;
        int letters;
        int printable;
        int nonPrintable;
        int controls;
        int allowedBlock;
        int maxRun;
        int maxFreq;

        double usefulRatio()        { return total == 0 ? 0.0 : (double) useful     / total; }
        double printableRatio()     { return total == 0 ? 0.0 : (double) printable  / total; }
        double controlRatio()       { return total == 0 ? 0.0 : (double) controls   / total; }
        double allowedBlockRatio()  { return total == 0 ? 0.0 : (double) allowedBlock / total; }
        double topCharShare()       { return total == 0 ? 0.0 : (double) maxFreq    / total; }
    }
}