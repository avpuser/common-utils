package com.avpuser.text_extract;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDSimpleFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.File;
import java.io.IOException;

public class PdfFontChecker {

    public static boolean pdfHasNoUnicodeMappingFonts(PDDocument document) {
        for (PDPage page : document.getPages()) {
            PDResources resources = page.getResources();
            if (resources == null) continue;

            for (COSName fontName : resources.getFontNames()) {
                PDFont font = null;
                try {
                    font = resources.getFont(fontName);
                } catch (IOException e) {
                    return true;
                }
                if (font == null) continue;

                // Попробуем проверить маппинг
                if (font instanceof PDSimpleFont || font instanceof PDType0Font) {
                    try {
                        String unicode = font.toUnicode(65); // произвольный код
                        if (unicode == null || unicode.trim().isEmpty()) {
                            return true; // хотя бы один символ не мапится
                        }
                    } catch (Exception e) {
                        return true; // ошибка при маппинге — тоже плохо
                    }
                }
            }
        }
        return false; // все ок
    }

    public static boolean isPdfReadableText(File file) {
        try (PDDocument doc = Loader.loadPDF(file)) {
            return !pdfHasNoUnicodeMappingFonts(doc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}