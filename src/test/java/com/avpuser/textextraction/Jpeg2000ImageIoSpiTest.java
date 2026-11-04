package com.avpuser.textextraction;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ensures jai-imageio-jpeg2000 is on the classpath and ImageIO discovers a JPEG2000 reader
 * (required by PDFBox for /JPXDecode streams).
 */
class Jpeg2000ImageIoSpiTest {

    @Test
    void imageIoDiscoversJpeg2000Reader() {
        Iterator<?> readers = ImageIO.getImageReadersByFormatName("JPEG2000");
        assertTrue(readers.hasNext(),
                "JPEG2000 ImageIO Reader missing — add com.github.jai-imageio:jai-imageio-jpeg2000");
    }
}
