package com.avpuser.textextraction;

import com.avpuser.test.MockTest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Regression for PDFBOX-5981: {@code page.getContents().readAllBytes()} truncated at the
 * JDK {@code readAllBytes} buffer boundary (16384) when a page has multiple content streams.
 * {@link PdfTextExtractor.PageContentStats#contentStreamBytes} uses that path via contentStreamSize().
 */
@MockTest
class PdfTextExtractorContentStreamSizeTest {

    private static final int JDK_READ_ALL_BYTES_BUFFER = 16384;

    @Test
    void contentStreamSize_multipleStreamsLargerThan16k_shouldNotTruncate() throws Exception {
        // Two streams whose payloads alone already exceed the buffer; plus one newline delimiter
        // per stream inserted by PDPage.getContentsForRandomAccess().
        int part1Len = 10000;
        int part2Len = 10000;
        byte[] part1 = commentStreamBytes(part1Len);
        byte[] part2 = commentStreamBytes(part2Len);
        Assertions.assertEquals(part1Len, part1.length);
        Assertions.assertEquals(part2Len, part2.length);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            List<PDStream> streams = Arrays.asList(
                    new PDStream(document, new ByteArrayInputStream(part1)),
                    new PDStream(document, new ByteArrayInputStream(part2)));
            page.setContents(streams);

            int expectedMin = part1Len + part2Len;
            Assertions.assertTrue(expectedMin > JDK_READ_ALL_BYTES_BUFFER,
                    "Precondition: raw stream bytes must exceed 16384");

            int viaGetContents;
            try (InputStream in = page.getContents()) {
                viaGetContents = in.readAllBytes().length;
            }
            Assertions.assertTrue(viaGetContents > JDK_READ_ALL_BYTES_BUFFER,
                    "getContents().readAllBytes() must not stop at 16384, got " + viaGetContents);
            Assertions.assertTrue(viaGetContents >= expectedMin,
                    "concatenated size must cover both streams, got " + viaGetContents);

            PdfTextExtractor.PageContentStats stats = PdfTextExtractor.PageContentStats.analyze(page);
            Assertions.assertEquals(viaGetContents, stats.contentStreamBytes,
                    "contentStreamSize path must match getContents().readAllBytes()");
            Assertions.assertNotEquals(JDK_READ_ALL_BYTES_BUFFER, stats.contentStreamBytes,
                    "must not be truncated to JDK buffer size (PDFBOX-5981)");
        }
    }

    /** PDF content-stream comment line of exact length (valid, ignored by operators). */
    private static byte[] commentStreamBytes(int length) {
        if (length < 2) {
            throw new IllegalArgumentException("length must be >= 2");
        }
        byte[] bytes = new byte[length];
        bytes[0] = '%';
        Arrays.fill(bytes, 1, length - 1, (byte) 'x');
        bytes[length - 1] = '\n';
        return bytes;
    }
}
