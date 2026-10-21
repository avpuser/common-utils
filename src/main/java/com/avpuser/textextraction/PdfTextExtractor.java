package com.avpuser.textextraction;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PdfTextExtractor {

    private static final Logger logger = LoggerFactory.getLogger(PdfTextExtractor.class);

    /**
     * Content stream larger than this (with fonts) strongly suggests drawn page content,
     * even when Unicode text extraction fails.
     */
    private static final int MIN_CONTENT_BYTES_WITH_FONTS = 32;

    /**
     * Content stream larger than this is treated as non-empty even without fonts/images
     * (vector drawings, complex layouts).
     */
    private static final int MIN_CONTENT_BYTES_SIGNIFICANT = 128;

    /**
     * If extracted text is only a short clean fragment but the page content stream is much larger,
     * treat as partial/broken extraction (e.g. only "KDL" / "Page 1" survived from a full lab page).
     * Truly short pages typically have ~30–50 byte content streams when drawing a single text line.
     */
    private static final int MIN_CONTENT_BYTES_SPARSE_MISMATCH = 256;

    /**
     * Minimum glyphs drawn on a page before unmapped-glyph ratio is considered reliable.
     */
    private static final int MIN_GLYPHS_FOR_UNMAPPED_CHECK = 40;

    /**
     * If this fraction of drawn glyphs has no usable ToUnicode mapping, the text layer is damaged
     * even when a readable remnant (e.g. English footer) was extracted.
     */
    private static final double MAX_UNMAPPED_GLYPH_RATIO = 0.40;

    /**
     * Image display area (from CTM) relative to page area — at/above this ⇒ full-page scan.
     */
    private static final float MIN_PAGE_IMAGE_COVER_RATIO = 0.45f;

    private enum PageVerdict {
        READABLE,
        SPARSE_OK,
        EMPTY,
        UNREADABLE
    }

    /**
     * Per-page extraction with readability gate.
     * Returns empty when any page has damaged/suspicious text or visual content without usable text,
     * so callers can fall back to full-document OCR.
     */
    private static Optional<String> extractTextFromDocument(PDDocument document) {
        try {
            int pageCount = document.getNumberOfPages();
            if (pageCount <= 0) {
                logger.warn("PDF has no pages");
                return Optional.empty();
            }

            PDFTextStripper stripper = new PDFTextStripper();

            List<String> acceptedPageTexts = new ArrayList<>();
            List<Integer> badPageNumbers = new ArrayList<>();
            List<String> badReasons = new ArrayList<>();
            int okPages = 0;
            int emptyPages = 0;

            for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
                stripper.setStartPage(pageIndex);
                stripper.setEndPage(pageIndex);
                String pageText = stripper.getText(document);
                PDPage page = document.getPage(pageIndex - 1);
                PageContentStats stats = PageContentStats.analyze(page);

                PageVerdict verdict = evaluatePage(pageText, page, stats);
                switch (verdict) {
                    case READABLE:
                    case SPARSE_OK:
                        acceptedPageTexts.add(pageText);
                        okPages++;
                        break;
                    case EMPTY:
                        emptyPages++;
                        break;
                    case UNREADABLE:
                        badPageNumbers.add(pageIndex);
                        badReasons.add(describeUnreadableReason(pageText, page, stats));
                        break;
                    default:
                        throw new IllegalStateException("Unexpected verdict: " + verdict);
                }
            }

            logger.info(
                    "PDF text extraction summary: pages={}, okPages={}, emptyPages={}, badPages={}, badReasons={}",
                    pageCount, okPages, emptyPages, badPageNumbers, badReasons);

            if (!badPageNumbers.isEmpty()) {
                logger.warn(
                        "PDF text quality gate failed — switching to OCR. badPages={}, reasons={}",
                        badPageNumbers, badReasons);
                return Optional.empty();
            }

            if (acceptedPageTexts.isEmpty()) {
                logger.info("PDF has no usable native text on any page — switching to OCR");
                return Optional.empty();
            }

            String combined = acceptedPageTexts.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining("\n\n"));
            return combined.isEmpty() ? Optional.empty() : Optional.of(combined);
        } catch (IOException e) {
            logger.error("Failed to extract text from PDF document", e);
            return Optional.empty();
        }
    }

    private static PageVerdict evaluatePage(String pageText, PDPage page, PageContentStats stats)
            throws IOException {
        if (pageText == null || pageText.isBlank()) {
            return evaluateBlankPage(page, stats);
        }

        // Drawn glyphs without ToUnicode — even if a readable remnant (footer) was extracted.
        if (hasSignificantUnmappedGlyphs(stats)) {
            return PageVerdict.UNREADABLE;
        }

        if (ReadableTextChecker.looksLikeReadableText(pageText)) {
            return PageVerdict.READABLE;
        }

        if (ReadableTextChecker.looksLikeCleanSparseText(pageText)) {
            if (stats.contentStreamBytes >= MIN_CONTENT_BYTES_SPARSE_MISMATCH) {
                return PageVerdict.UNREADABLE;
            }
            return PageVerdict.SPARSE_OK;
        }

        return PageVerdict.UNREADABLE;
    }

    /**
     * Blank extracted text: distinguish empty/stamp pages from pages that still have
     * drawn glyphs or a full-page scan (prefer OCR for those).
     * Small images alone (seals, signatures) do not force OCR and must not hide drawn glyphs
     * (e.g. broken text inside a Form XObject next to a logo).
     */
    private static PageVerdict evaluateBlankPage(PDPage page, PageContentStats stats) throws IOException {
        // Any actually painted glyphs with no extractable text ⇒ damaged / incomplete text layer.
        if (stats.glyphCount > 0) {
            return PageVerdict.UNREADABLE;
        }

        if (pageHasFonts(page) && stats.contentStreamBytes >= MIN_CONTENT_BYTES_WITH_FONTS) {
            return PageVerdict.UNREADABLE;
        }

        // Substantial vector content without explaining images (decorative stamp pages often
        // have a small image + short stream; keep those as EMPTY via the image exemption).
        if (stats.contentStreamBytes >= MIN_CONTENT_BYTES_SIGNIFICANT && !pageHasImageXObject(page)) {
            return PageVerdict.UNREADABLE;
        }

        if (stats.hasFullPageRaster()) {
            return PageVerdict.UNREADABLE;
        }

        return PageVerdict.EMPTY;
    }

    private static boolean hasSignificantUnmappedGlyphs(PageContentStats stats) {
        return stats.glyphCount >= MIN_GLYPHS_FOR_UNMAPPED_CHECK
                && stats.unmappedGlyphRatio() >= MAX_UNMAPPED_GLYPH_RATIO;
    }

    private static String describeUnreadableReason(String pageText, PDPage page, PageContentStats stats) {
        if (pageText == null || pageText.isBlank()) {
            if (stats.glyphCount > 0) {
                return "blank_text_with_drawn_glyphs";
            }
            if (stats.hasFullPageRaster()) {
                return "blank_text_with_full_page_raster";
            }
            return "blank_text_with_drawn_content";
        }
        if (hasSignificantUnmappedGlyphs(stats)) {
            return "unmapped_glyphs_ratio_high";
        }
        if (ReadableTextChecker.looksLikeCleanSparseText(pageText)
                && stats.contentStreamBytes >= MIN_CONTENT_BYTES_SPARSE_MISMATCH) {
            return "sparse_text_mismatched_to_page_content";
        }
        if (ReadableTextChecker.looksLikeGibberish(pageText)) {
            return "gibberish_or_undetectable_language";
        }
        return "low_quality_extracted_text";
    }

    private static int contentStreamSize(PDPage page) throws IOException {
        try (InputStream in = page.getContents()) {
            if (in == null) {
                return 0;
            }
            return in.readAllBytes().length;
        }
    }

    private static boolean pageHasFonts(PDPage page) throws IOException {
        PDResources resources = page.getResources();
        if (resources == null) {
            return false;
        }
        for (COSName ignored : resources.getFontNames()) {
            return true;
        }
        return false;
    }

    private static boolean pageHasImageXObject(PDPage page) throws IOException {
        return pageHasImageXObject(page.getResources(), 0);
    }

    private static boolean pageHasImageXObject(PDResources resources, int depth) throws IOException {
        if (resources == null || depth > 5) {
            return false;
        }
        for (COSName name : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(name);
            if (xObject instanceof PDImageXObject) {
                return true;
            }
            if (xObject instanceof PDFormXObject form) {
                if (pageHasImageXObject(form.getResources(), depth + 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * One pass over the page content stream: counts glyphs with/without ToUnicode and
     * measures image display area via the current transformation matrix (not pixel size).
     */
    static final class PageContentStats {
        final int glyphCount;
        final int unmappedGlyphCount;
        final float maxImageDisplayArea;
        final float pageArea;
        /** Page content stream size (not including nested Form streams). Measured once per page. */
        final int contentStreamBytes;

        private PageContentStats(int glyphCount, int unmappedGlyphCount,
                                 float maxImageDisplayArea, float pageArea, int contentStreamBytes) {
            this.glyphCount = glyphCount;
            this.unmappedGlyphCount = unmappedGlyphCount;
            this.maxImageDisplayArea = maxImageDisplayArea;
            this.pageArea = pageArea;
            this.contentStreamBytes = contentStreamBytes;
        }

        static PageContentStats analyze(PDPage page) throws IOException {
            PDRectangle box = page.getCropBox() != null ? page.getCropBox() : page.getMediaBox();
            float pageArea = box == null ? 0f : Math.max(1f, box.getWidth() * box.getHeight());
            int contentStreamBytes = contentStreamSize(page);
            ContentProbe probe = new ContentProbe(page);
            probe.processPage(page);
            return new PageContentStats(
                    probe.glyphCount,
                    probe.unmappedGlyphCount,
                    probe.maxImageDisplayArea,
                    pageArea,
                    contentStreamBytes);
        }

        double unmappedGlyphRatio() {
            return glyphCount == 0 ? 0.0 : (double) unmappedGlyphCount / glyphCount;
        }

        boolean hasFullPageRaster() {
            return pageArea > 0f && (maxImageDisplayArea / pageArea) >= MIN_PAGE_IMAGE_COVER_RATIO;
        }
    }

    /**
     * Lightweight content-stream probe. Does not render; only observes glyphs and image CTMs.
     * Form XObjects are processed by PDFGraphicsStreamEngine's DrawObject recursively.
     */
    private static final class ContentProbe extends PDFGraphicsStreamEngine {
        private int glyphCount;
        private int unmappedGlyphCount;
        private float maxImageDisplayArea;

        private ContentProbe(PDPage page) {
            super(page);
        }

        @Override
        protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement)
                throws IOException {
            glyphCount++;
            if (!hasUsableUnicode(font, code)) {
                unmappedGlyphCount++;
            }
        }

        private static boolean hasUsableUnicode(PDFont font, int code) {
            if (font == null) {
                return false;
            }
            try {
                String unicode = font.toUnicode(code);
                if (unicode == null || unicode.isEmpty()) {
                    return false;
                }
                return !unicode.codePoints().allMatch(cp -> cp == 0xFFFD);
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public void drawImage(PDImage image) {
            // Image is painted into the unit square transformed by CTM → display size in page space.
            Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
            float width = Math.abs(ctm.getScalingFactorX());
            float height = Math.abs(ctm.getScalingFactorY());
            maxImageDisplayArea = Math.max(maxImageDisplayArea, width * height);
        }

        @Override
        public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
        }

        @Override
        public void clip(int windingRule) {
        }

        @Override
        public void moveTo(float x, float y) {
        }

        @Override
        public void lineTo(float x, float y) {
        }

        @Override
        public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        }

        @Override
        public Point2D getCurrentPoint() {
            return new Point2D.Float();
        }

        @Override
        public void closePath() {
        }

        @Override
        public void endPath() {
        }

        @Override
        public void strokePath() {
        }

        @Override
        public void fillPath(int windingRule) {
        }

        @Override
        public void fillAndStrokePath(int windingRule) {
        }

        @Override
        public void shadingFill(COSName shadingName) {
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
            logger.error("Failed to load PDF from file", e);
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
            logger.error("Failed to load PDF from URL", e);
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

    public static int countPages(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return document.getNumberOfPages();
        } catch (Exception e) {
            logger.error("Failed to count PDF pages, fallback to 1 page");
            return 1;
        }
    }

    public static int countPages(String filePath) {
        File file = new File(filePath);
        try (PDDocument document = Loader.loadPDF(file)) {
            return document.getNumberOfPages();
        } catch (Exception e) {
            logger.error("Failed to count PDF pages, fallback to 1 page");
            return 1;
        }
    }
}
