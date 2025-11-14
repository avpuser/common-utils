package com.avpuser.textextraction;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PdfRenderingService {

    private static final Logger logger = LoggerFactory.getLogger(PdfRenderingService.class);

    public static List<byte[]> renderPdfToPng(byte[] pdfBytes, int dpi) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            logger.warn("renderPdfToPng called with empty PDF bytes");
            return Collections.emptyList();
        }

        if (dpi <= 0) {
            throw new IllegalArgumentException("dpi must be greater than 0");
        }

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            int pageCount = doc.getNumberOfPages();
            List<byte[]> renderedPages = new ArrayList<>(pageCount);

            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, dpi, ImageType.RGB);
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    ImageIO.write(image, "png", outputStream);
                    renderedPages.add(outputStream.toByteArray());
                }
            }

            return renderedPages;
        } catch (IOException e) {
            logger.error("Failed to render PDF to PNG", e);
            return Collections.emptyList();
        }
    }
}
