package com.avpuser.textextraction;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;

public class PdfTextExtractor {

    private static final Logger logger = LoggerFactory.getLogger(PdfTextExtractor.class);

    /**
     * Общая логика извлечения текста из PDF-документа
     */
    private static Optional<String> extractTextFromDocument(PDDocument document) {
        try {
            if (PdfFontChecker.pdfHasNoUnicodeMappingFonts(document)) {
                logger.warn("PDF contains fonts without Unicode mapping — text is likely not extractable.");
                return Optional.empty();
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document).trim();

            return text.isBlank() ? Optional.empty() : Optional.of(text);
        } catch (IOException e) {
            logger.error("Failed to extract text from PDF document", e);
            return Optional.empty();
        }
    }

    /**
     * Извлечение текста из файла на диске
     */
    public static Optional<String> extractTextFromPdf(String filePath) {
        File file = new File(filePath);
        try (PDDocument document = Loader.loadPDF(file)) {
            return extractTextFromDocument(document);
        } catch (IOException e) {
            logger.error("Failed to load PDF from file: " + filePath, e);
            return Optional.empty();
        }
    }

    /**
     * Извлечение текста из PDF по URL
     */
    public static Optional<String> extractTextFromPdfUrl(String fileUrl) {
        try (InputStream in = new URL(fileUrl).openStream()) {
            byte[] pdfBytes = in.readAllBytes();
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                return extractTextFromDocument(document);
            }
        } catch (IOException e) {
            logger.error("Failed to load PDF from URL: " + fileUrl, e);
            return Optional.empty();
        }
    }

    /**
     * Извлечение текста из PDF-документа по байтам (например, из MultipartFile)
     */
    public static Optional<String> extractTextFromPdfBytes(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return extractTextFromDocument(document);
        } catch (IOException e) {
            logger.error("Failed to load PDF from byte array", e);
            return Optional.empty();
        }
    }

}
