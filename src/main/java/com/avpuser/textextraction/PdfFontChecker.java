package com.avpuser.textextraction;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDSimpleFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;

public class PdfFontChecker {

    public static boolean pdfHasNoUnicodeMappingFonts(PDDocument document) {
        // 1) Попробуем честно извлечь текст
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            if (looksLikeReadableText(text)) {
                // Текст реально читается — значит, для наших целей PDF «нормальный»
                return false;
            }
        } catch (IOException e) {
            // игнорируем — упадём к проверке шрифтов
        }

        // 2) Текст не извлёкся/слишком слабый — проверим шрифты щадяще
        boolean anyFontHasMapping = false;

        for (PDPage page : document.getPages()) {
            PDResources resources = page.getResources();
            if (resources == null) continue;

            for (COSName fontName : resources.getFontNames()) {
                PDFont font;
                try {
                    font = resources.getFont(fontName);
                } catch (IOException e) {
                    // не смогли прочитать шрифт — не делаем выводов, идём дальше
                    continue;
                }
                if (font == null) continue;

                try {
                    if (font instanceof PDSimpleFont) {
                        // Для простых шрифтов возьмём реальную карту кодов из encoding
                        var enc = ((PDSimpleFont) font).getEncoding();
                        if (enc != null && enc.getCodeToNameMap() != null) {
                            for (Integer code : enc.getCodeToNameMap().keySet()) {
                                if (code == null) continue;
                                String uni = safeToUnicode(font, code);
                                if (isNonEmpty(uni)) {
                                    anyFontHasMapping = true;
                                    break;
                                }
                            }
                        }
                    } else if (font instanceof PDType0Font) {
                        // Для композитных шрифтов нет простого доступа к карте кодов.
                        // Пробуем несколько типичных кодов (пробел, цифры, латиница, кириллица).
                        int[] probeCodes = new int[]{
                                32, // space
                                48, 49, 50, // '0','1','2'
                                65, 97, // 'A','a'
                                1040, 1072, // 'А','а' (кириллица в Unicode)
                                1043, 1088 // 'Г','р' — просто ещё пара кириллических
                        };
                        for (int code : probeCodes) {
                            String uni = safeToUnicode(font, code);
                            if (isNonEmpty(uni)) {
                                anyFontHasMapping = true;
                                break;
                            }
                        }
                    } else {
                        // На всякий случай: попробуем пару универсальных кодов
                        for (int code : new int[]{32, 48, 65, 97}) {
                            String uni = safeToUnicode(font, code);
                            if (isNonEmpty(uni)) {
                                anyFontHasMapping = true;
                                break;
                            }
                        }
                    }
                } catch (Exception ignore) {
                    // Любые проблемы с конкретным шрифтом — не делаем жёстких выводов
                }

                if (anyFontHasMapping) break;
            }
            if (anyFontHasMapping) break;
        }

        // Если ни один шрифт не дал ни одного валидного маппинга и текста мы не вытащили — считаем, что «нет Unicode-маппинга».
        return !anyFontHasMapping;
    }

    /**
     * Эвристика: текст «похож на читаемый», если он не пустой и содержит достаточное число букв/цифр.
     */
    private static boolean looksLikeReadableText(String text) {
        if (!isNonEmpty(text)) return false;

        // считаем «полезные» символы: буквы/цифры в широком смысле (Unicode)
        int useful = 0;
        int total = text.length();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch)) useful++;
        }

        // минимальные пороги: хотя бы 40 полезных символов И не меньше 10% от общего
        return useful >= 40 && useful * 10 >= total;
    }

    private static boolean isNonEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /**
     * Безопасный вызов toUnicode: некоторые шрифты бросают исключения/возвращают null.
     */
    private static String safeToUnicode(PDFont font, int code) {
        try {
            String uni = font.toUnicode(code);
            if (uni == null) return null;
            // отбрасываем невидимые/пробельные
            String trimmed = uni.trim();
            if (trimmed.isEmpty()) return null;
            // Часто попадается \uFFFD (replacement char) — тоже считаем «пустым»
            if (trimmed.codePoints().allMatch(cp -> cp == 0xFFFD)) return null;
            return trimmed;
        } catch (Exception e) {
            return null;
        }
    }
}