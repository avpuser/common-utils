package com.avpuser.mongo.encryption;

import com.avpuser.mongo.encryption.exception.EncryptedFieldDecryptionException;
import com.avpuser.mongo.encryption.exception.MalformedEncryptedPayloadException;
import com.avpuser.mongo.encryption.exception.UnknownEncryptionKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PiiEncryptionServiceTest {

    private static final String CONTEXT = "user_v2:contactEmail";
    private static final String OTHER_CONTEXT = "user_v2:displayName";

    private String activeKeyBase64;
    private String lookupKeyBase64;
    private PiiEncryptionService service;

    @BeforeEach
    void setUp() {
        activeKeyBase64 = EncryptionKeyConfig.generateRandomAesKeyBase64();
        lookupKeyBase64 = EncryptionKeyConfig.generateRandomHmacKeyBase64();
        EncryptionKeyConfig config = EncryptionKeyConfig.create("pii-v1", activeKeyBase64, Map.of(), lookupKeyBase64);
        service = new PiiEncryptionService(config);
    }

    // ---- encryption/decryption ----

    @Test
    void encrypt_thenDecrypt_returnsOriginalPlaintext() {
        String plaintext = "user@example.com";
        String encrypted = service.encrypt(plaintext, CONTEXT);
        assertNotEquals(plaintext, encrypted);
        assertEquals(plaintext, service.decrypt(encrypted, CONTEXT));
    }

    @Test
    void encrypt_producesEnvelopeWithExpectedPrefixAndKeyId() {
        String encrypted = service.encrypt("secret", CONTEXT);
        assertTrue(encrypted.startsWith("msenc:v1:pii-v1:"));
    }

    @Test
    void sameValueEncryptedTwice_producesDifferentCiphertext_bothDecryptCorrectly() {
        String plaintext = "user@example.com";
        String encrypted1 = service.encrypt(plaintext, CONTEXT);
        String encrypted2 = service.encrypt(plaintext, CONTEXT);

        assertNotEquals(encrypted1, encrypted2);
        assertEquals(plaintext, service.decrypt(encrypted1, CONTEXT));
        assertEquals(plaintext, service.decrypt(encrypted2, CONTEXT));
    }

    @Test
    void nonceDiffersBetweenCalls() {
        String encrypted1 = service.encrypt("same-plaintext", CONTEXT);
        String encrypted2 = service.encrypt("same-plaintext", CONTEXT);
        String nonce1 = encrypted1.split(":", 5)[3];
        String nonce2 = encrypted2.split(":", 5)[3];
        assertNotEquals(nonce1, nonce2);
    }

    @Test
    void supportsUnicodeAndCyrillic() {
        String plaintext = "Иван Петров <тест@пример.рф> 🎉";
        String encrypted = service.encrypt(plaintext, CONTEXT);
        assertEquals(plaintext, service.decrypt(encrypted, CONTEXT));
    }

    @Test
    void supportsLongStrings() {
        String plaintext = "a".repeat(10_000);
        String encrypted = service.encrypt(plaintext, CONTEXT);
        assertEquals(plaintext, service.decrypt(encrypted, CONTEXT));
    }

    @Test
    void emptyString_isEncryptedLikeAnyOtherPlaintext() {
        String encrypted = service.encrypt("", CONTEXT);
        assertNotEquals("", encrypted);
        assertEquals("", service.decrypt(encrypted, CONTEXT));
    }

    @Test
    void null_isNotEncrypted() {
        assertNull(service.encrypt(null, CONTEXT));
    }

    @Test
    void null_decryptsToNull() {
        assertNull(service.decrypt(null, CONTEXT));
    }

    // ---- corruption / tampering ----

    @Test
    void corruptedCiphertext_throwsDecryptionException() {
        String encrypted = service.encrypt("user@example.com", CONTEXT);
        String[] parts = encrypted.split(":", 5);
        // Appending well-formed base64url characters keeps the payload structurally parseable
        // (still decodes), but the appended bytes break GCM tag verification.
        String corrupted = String.join(":", parts[0], parts[1], parts[2], parts[3], parts[4] + "AAAA");
        assertThrows(EncryptedFieldDecryptionException.class, () -> service.decrypt(corrupted, CONTEXT));
    }

    @Test
    void corruptedAuthTag_throwsDecryptionException() {
        String encrypted = service.encrypt("user@example.com", CONTEXT);
        String[] parts = encrypted.split(":", 5);
        byte[] ciphertext = java.util.Base64.getUrlDecoder().decode(parts[4]);
        ciphertext[ciphertext.length - 1] ^= 0x01; // flip last byte (part of the GCM tag)
        String tampered = parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3] + ":"
                + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(ciphertext);
        assertThrows(EncryptedFieldDecryptionException.class, () -> service.decrypt(tampered, CONTEXT));
    }

    @Test
    void corruptedNonce_throwsDecryptionOrMalformedException() {
        String encrypted = service.encrypt("user@example.com", CONTEXT);
        String[] parts = encrypted.split(":", 5);
        byte[] nonce = java.util.Base64.getUrlDecoder().decode(parts[3]);
        nonce[0] ^= 0x01;
        String tampered = parts[0] + ":" + parts[1] + ":" + parts[2] + ":"
                + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(nonce) + ":" + parts[4];
        assertThrows(EncryptedFieldDecryptionException.class, () -> service.decrypt(tampered, CONTEXT));
    }

    @Test
    void wrongNonceLength_throwsMalformedException() {
        String malformed = "msenc:v1:pii-v1:" + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[4])
                + ":" + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
        assertThrows(MalformedEncryptedPayloadException.class, () -> service.decrypt(malformed, CONTEXT));
    }

    @Test
    void wrongKey_throwsDecryptionException() {
        String encrypted = service.encrypt("user@example.com", CONTEXT);

        EncryptionKeyConfig otherConfig = EncryptionKeyConfig.create(
                "pii-v1", EncryptionKeyConfig.generateRandomAesKeyBase64(), Map.of(), lookupKeyBase64);
        PiiEncryptionService otherService = new PiiEncryptionService(otherConfig);

        assertThrows(EncryptedFieldDecryptionException.class, () -> otherService.decrypt(encrypted, CONTEXT));
    }

    @Test
    void unknownKeyId_throwsUnknownEncryptionKeyException() {
        String foreignEnvelope = "msenc:v1:some-other-key-id:"
                + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[12])
                + ":" + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
        UnknownEncryptionKeyException ex = assertThrows(UnknownEncryptionKeyException.class,
                () -> service.decrypt(foreignEnvelope, CONTEXT));
        assertEquals("some-other-key-id", ex.getKeyId());
    }

    @Test
    void unknownVersion_throwsMalformedException() {
        String encrypted = service.encrypt("user@example.com", CONTEXT);
        String withBadVersion = encrypted.replaceFirst(":v1:", ":v99:");
        assertThrows(MalformedEncryptedPayloadException.class, () -> service.decrypt(withBadVersion, CONTEXT));
    }

    @Test
    void malformedBase64_throwsMalformedException() {
        String malformed = "msenc:v1:pii-v1:not-valid-base64-!!!:also-not-base64-!!!";
        assertThrows(MalformedEncryptedPayloadException.class, () -> service.decrypt(malformed, CONTEXT));
    }

    @Test
    void wrongSegmentCount_throwsMalformedException() {
        assertThrows(MalformedEncryptedPayloadException.class, () -> service.decrypt("msenc:v1:pii-v1", CONTEXT));
        assertThrows(MalformedEncryptedPayloadException.class, () -> service.decrypt("msenc:v1", CONTEXT));
    }

    @Test
    void ciphertextMovedToAnotherContext_failsAuthentication() {
        String encrypted = service.encrypt("user@example.com", CONTEXT);
        assertThrows(EncryptedFieldDecryptionException.class, () -> service.decrypt(encrypted, OTHER_CONTEXT));
    }

    // ---- double encryption guard ----

    @Test
    void encryptingAlreadyEncryptedValue_doesNotReEncrypt() {
        String encrypted = service.encrypt("user@example.com", CONTEXT);
        String encryptedAgain = service.encrypt(encrypted, CONTEXT);
        assertEquals(encrypted, encryptedAgain);
        assertEquals("user@example.com", service.decrypt(encryptedAgain, CONTEXT));
    }

    @Test
    void malformedEnvelopeLookingPlaintext_isTreatedAsPlaintextAndEncrypted() {
        // Has the "msenc:" prefix by coincidence, but is not a structurally valid envelope.
        String weirdPlaintext = "msenc:not-a-real-envelope";
        String encrypted = service.encrypt(weirdPlaintext, CONTEXT);
        assertNotEquals(weirdPlaintext, encrypted);
        assertEquals(weirdPlaintext, service.decrypt(encrypted, CONTEXT));
    }

    @Test
    void encryptDecryptEncrypt_roundTripsCorrectly() {
        String plaintext = "user@example.com";
        String encrypted1 = service.encrypt(plaintext, CONTEXT);
        String decrypted = service.decrypt(encrypted1, CONTEXT);
        String encrypted2 = service.encrypt(decrypted, CONTEXT);
        assertEquals(plaintext, service.decrypt(encrypted2, CONTEXT));
    }

    // ---- double decryption guard ----

    @Test
    void decryptingPlaintext_returnsPlaintextUnchanged() {
        assertEquals("plain-value", service.decrypt("plain-value", CONTEXT));
    }

    @Test
    void decryptingTwice_secondCallIsNoOp() {
        String encrypted = service.encrypt("user@example.com", CONTEXT);
        String decryptedOnce = service.decrypt(encrypted, CONTEXT);
        String decryptedTwice = service.decrypt(decryptedOnce, CONTEXT);
        assertEquals(decryptedOnce, decryptedTwice);
    }

    @Test
    void corruptedEnvelope_isNeverSilentlyReturnedAsPlaintext() {
        String corrupted = "msenc:v1:pii-v1:###:###";
        assertThrows(MalformedEncryptedPayloadException.class, () -> service.decrypt(corrupted, CONTEXT));
    }

    // ---- isValidEnvelope ----

    @Test
    void isValidEnvelope_trueForRealEnvelope_falseForPlaintext() {
        String encrypted = service.encrypt("user@example.com", CONTEXT);
        assertTrue(service.isValidEnvelope(encrypted));
        assertFalse(service.isValidEnvelope("user@example.com"));
        assertFalse(service.isValidEnvelope("msenc:not-a-real-envelope"));
    }

    // ---- lookup ----

    @Test
    void computeLookup_isDeterministic() {
        String lookup1 = service.computeLookup("user@example.com", CONTEXT);
        String lookup2 = service.computeLookup("user@example.com", CONTEXT);
        assertEquals(lookup1, lookup2);
    }

    @Test
    void computeLookup_differsByContext() {
        String lookup1 = service.computeLookup("user@example.com", CONTEXT);
        String lookup2 = service.computeLookup("user@example.com", OTHER_CONTEXT);
        assertNotEquals(lookup1, lookup2);
    }
}
