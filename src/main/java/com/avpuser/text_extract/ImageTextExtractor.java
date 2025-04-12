package com.avpuser.text_extract;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class ImageTextExtractor {

    private static final Tesseract tesseract = new Tesseract();

    static {
        // Установи путь к папке с языковыми моделями (tessdata)
        tesseract.setDatapath("tessdata");
        tesseract.setLanguage("rus+eng"); // если нужно, можно изменить
    }

    // Метод для извлечения текста из изображения по URL
    public static String extractTextFromImageUrl(String imageUrl) {
        try (InputStream in = new URL(imageUrl).openStream()) {
            BufferedImage image = ImageIO.read(in);
            return tesseract.doOCR(image);
        } catch (IOException | TesseractException e) {
            throw new RuntimeException("Ошибка OCR по URL: " + e.getMessage(), e);
        }
    }

    // Метод для извлечения текста из локального файла
    public static String extractTextFromImageFile(String filePath) {
        try {
            BufferedImage image = ImageIO.read(new File(filePath));
            return tesseract.doOCR(image);
        } catch (IOException | TesseractException e) {
            throw new RuntimeException("Ошибка OCR по файлу: " + e.getMessage(), e);
        }
    }
}