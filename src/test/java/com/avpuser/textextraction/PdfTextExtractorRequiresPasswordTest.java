package com.avpuser.textextraction;

import com.avpuser.test.MockTest;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MockTest
class PdfTextExtractorRequiresPasswordTest {

    @Test
    void plainPdf_doesNotRequirePassword() throws IOException {
        assertFalse(PdfTextExtractor.requiresPassword(plainPdf()));
    }

    @Test
    void userPasswordPdf_rc4_requiresPassword() throws IOException {
        assertTrue(PdfTextExtractor.requiresPassword(encrypt(plainPdf(), "owner", "user", 128)));
    }

    @Test
    void userPasswordPdf_aes256_requiresPassword() throws IOException {
        assertTrue(PdfTextExtractor.requiresPassword(encrypt(plainPdf(), "owner", "user", 256)));
    }

    @Test
    void ownerOnlyEncryptedPdf_opensWithEmptyPassword_doesNotRequirePassword() throws IOException {
        byte[] ownerOnly = encrypt(plainPdf(), "owner", "", 256);

        try (PDDocument document = Loader.loadPDF(ownerOnly)) {
            assertTrue(document.isEncrypted());
        }
        assertFalse(PdfTextExtractor.requiresPassword(ownerOnly));
    }

    @Test
    void truncatedPdf_doesNotRequirePassword() throws IOException {
        byte[] pdf = plainPdf();
        assertFalse(PdfTextExtractor.requiresPassword(Arrays.copyOf(pdf, pdf.length / 2)));
    }

    @Test
    void garbageAfterPdfHeader_doesNotRequirePassword() {
        byte[] garbage = "%PDF-1.7\nnot really a pdf at all".getBytes(StandardCharsets.US_ASCII);
        assertFalse(PdfTextExtractor.requiresPassword(garbage));
    }

    private static byte[] plainPdf() throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private static byte[] encrypt(byte[] pdf, String ownerPassword, String userPassword, int keyLength) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            StandardProtectionPolicy policy = new StandardProtectionPolicy(ownerPassword, userPassword, new AccessPermission());
            policy.setEncryptionKeyLength(keyLength);
            document.protect(policy);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
