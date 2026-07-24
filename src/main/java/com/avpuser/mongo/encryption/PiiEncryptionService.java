package com.avpuser.mongo.encryption;

import com.avpuser.mongo.encryption.exception.EncryptedFieldDecryptionException;
import com.avpuser.mongo.encryption.exception.MalformedEncryptedPayloadException;
import com.avpuser.mongo.encryption.exception.UnknownEncryptionKeyException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Transparent field-level encryption for MongoDB entities: AES-256-GCM with a random 12-byte
 * nonce per call and a 128-bit authentication tag, plus HMAC-SHA-256 blind-index computation for
 * exact-match lookup. Never uses ECB mode, a fixed nonce, or deterministic AES-GCM.
 * <p>
 * Ciphertext is stored as a self-describing envelope string:
 * {@code msenc:v1:<keyId>:<base64url-nonce>:<base64url-ciphertext-with-tag>}. The field's stable
 * context string (see {@link Encrypted#context()}) is bound in as AEAD Additional Authenticated
 * Data, so ciphertext copied into a different field or collection fails authentication rather
 * than decrypting to garbage.
 * <p>
 * {@link #encrypt(String, String)} is idempotent on already-valid envelopes (returns them
 * unchanged) to guard against double encryption, including on partial (`$set`-style) updates and
 * on re-saving an entity that was read, decrypted, left untouched, and saved again.
 * {@link #decrypt(String, String)} is idempotent on plaintext (returns it unchanged) but never
 * silently accepts a corrupted or foreign-keyed envelope as plaintext.
 */
public class PiiEncryptionService {

    private static final Logger logger = LogManager.getLogger(PiiEncryptionService.class);

    private static final String ENVELOPE_PREFIX = "msenc";
    private static final String VERSION_1 = "v1";
    private static final int NONCE_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_TAG_LENGTH_BYTES = GCM_TAG_LENGTH_BITS / 8;
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";

    private final EncryptionKeyConfig keyConfig;
    private final SecureRandom secureRandom = new SecureRandom();

    public PiiEncryptionService(EncryptionKeyConfig keyConfig) {
        if (keyConfig == null) {
            throw new IllegalArgumentException("keyConfig must not be null");
        }
        this.keyConfig = keyConfig;
    }

    /**
     * Encrypts {@code plaintext} with the active key, binding {@code context} as AAD.
     *
     * @return {@code null} for {@code null} input; {@code plaintext} unchanged if it is already a
     * structurally valid envelope (double-encryption guard); otherwise a new
     * {@code msenc:v1:...} envelope with a fresh random nonce
     */
    public String encrypt(String plaintext, String context) {
        if (plaintext == null) {
            return null;
        }
        requireContext(context);

        if (isValidEnvelope(plaintext)) {
            // Already encrypted (possibly with a legacy or even unrecognized keyId): never
            // re-encrypt. Guards against double encryption on re-save, partial updates, etc.
            return plaintext;
        }

        try {
            byte[] nonce = new byte[NONCE_LENGTH_BYTES];
            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            SecretKey activeKey = keyConfig.getActiveKey();
            cipher.init(Cipher.ENCRYPT_MODE, activeKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return ENVELOPE_PREFIX + ":" + VERSION_1 + ":" + keyConfig.getActiveKeyId() + ":"
                    + urlEncode(nonce) + ":" + urlEncode(ciphertext);
        } catch (GeneralSecurityException e) {
            // Programming/config error (bad key, unsupported algorithm), not user data - fail loudly.
            throw new IllegalStateException("PII field encryption failed unexpectedly", e);
        }
    }

    /**
     * Decrypts {@code value} previously produced by {@link #encrypt(String, String)}, verifying
     * that {@code context} matches the AAD used at encryption time.
     *
     * @return {@code null} for {@code null} input; {@code value} unchanged if it does not look
     * like an envelope at all (legacy plaintext, or already-decrypted plaintext passed back in)
     * @throws MalformedEncryptedPayloadException if the value carries the envelope prefix but its
     *                                             structure, version, nonce, or ciphertext length is invalid
     * @throws UnknownEncryptionKeyException       if the envelope's keyId is not configured here
     *                                             (active or legacy)
     * @throws EncryptedFieldDecryptionException   if decryption/authentication fails (wrong key,
     *                                             corrupted ciphertext/nonce, or context/AAD mismatch)
     */
    public String decrypt(String value, String context) {
        if (value == null) {
            return null;
        }
        requireContext(context);

        if (!looksLikeEnvelope(value)) {
            // No envelope prefix at all: treat as legacy/plain plaintext.
            return value;
        }

        Envelope envelope = parseEnvelopeOrThrow(value);

        SecretKey key = keyConfig.getKeyById(envelope.keyId);
        if (key == null) {
            logger.warn("PII decrypt failed: unknown encryption keyId. context={}, keyId={}", context, envelope.keyId);
            throw new UnknownEncryptionKeyException(
                    "Unknown PII encryption keyId '" + envelope.keyId + "' for context '" + context + "'",
                    envelope.keyId);
        }

        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, envelope.nonce));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            byte[] plaintext = cipher.doFinal(envelope.ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            logger.warn("PII decrypt failed: authentication tag mismatch. context={}, keyId={}", context, envelope.keyId);
            throw new EncryptedFieldDecryptionException(
                    "Authentication failed while decrypting PII field for context '" + context
                            + "' (wrong key, corrupted data, or mismatched context)", e);
        } catch (GeneralSecurityException e) {
            logger.warn("PII decrypt failed. context={}, keyId={}", context, envelope.keyId);
            throw new EncryptedFieldDecryptionException(
                    "Failed to decrypt PII field for context '" + context + "'", e);
        }
    }

    /** Computes the HMAC-SHA-256 blind-index value for {@code normalizedValue} under {@code context}. */
    public String computeLookup(String normalizedValue, String context) {
        return LookupHashService.computeLookup(normalizedValue, context, keyConfig.getLookupKey());
    }

    /**
     * True if {@code value} is a structurally valid {@code msenc:v1:...} envelope: correct prefix
     * and version, non-blank keyId, nonce that decodes to exactly {@value #NONCE_LENGTH_BYTES}
     * bytes, and ciphertext that decodes to more than the GCM tag length. Does not require the
     * keyId to be configured here (a foreign/legacy keyId still counts as "already encrypted" for
     * double-encryption purposes) and does not verify the authentication tag.
     */
    public boolean isValidEnvelope(String value) {
        if (!looksLikeEnvelope(value)) {
            return false;
        }
        try {
            parseEnvelopeOrThrow(value);
            return true;
        } catch (MalformedEncryptedPayloadException e) {
            return false;
        }
    }

    private boolean looksLikeEnvelope(String value) {
        return value.startsWith(ENVELOPE_PREFIX + ":");
    }

    private Envelope parseEnvelopeOrThrow(String value) {
        String[] parts = value.split(":", 5);
        if (parts.length != 5 || !ENVELOPE_PREFIX.equals(parts[0])) {
            throw new MalformedEncryptedPayloadException(
                    "Malformed PII encrypted payload: expected 5 ':'-separated segments");
        }
        String version = parts[1];
        String keyId = parts[2];
        String nonceB64 = parts[3];
        String ciphertextB64 = parts[4];

        if (!VERSION_1.equals(version)) {
            throw new MalformedEncryptedPayloadException(
                    "Malformed PII encrypted payload: unsupported envelope version '" + version + "'");
        }
        if (keyId.isBlank()) {
            throw new MalformedEncryptedPayloadException("Malformed PII encrypted payload: blank keyId");
        }

        byte[] nonce = decodeUrlBase64OrThrow(nonceB64, "nonce");
        if (nonce.length != NONCE_LENGTH_BYTES) {
            throw new MalformedEncryptedPayloadException(
                    "Malformed PII encrypted payload: nonce must be " + NONCE_LENGTH_BYTES
                            + " bytes, was " + nonce.length);
        }

        byte[] ciphertext = decodeUrlBase64OrThrow(ciphertextB64, "ciphertext");
        if (ciphertext.length < GCM_TAG_LENGTH_BYTES) {
            // Exactly GCM_TAG_LENGTH_BYTES is valid: it's what an empty plaintext encrypts to
            // (zero content bytes + the auth tag).
            throw new MalformedEncryptedPayloadException(
                    "Malformed PII encrypted payload: ciphertext too short to contain a GCM auth tag");
        }

        return new Envelope(keyId, nonce, ciphertext);
    }

    private byte[] decodeUrlBase64OrThrow(String base64Value, String label) {
        try {
            return Base64.getUrlDecoder().decode(base64Value);
        } catch (IllegalArgumentException e) {
            throw new MalformedEncryptedPayloadException(
                    "Malformed PII encrypted payload: invalid Base64 in " + label, e);
        }
    }

    private String urlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void requireContext(String context) {
        if (context == null || context.isBlank()) {
            throw new IllegalArgumentException("context must not be blank");
        }
    }

    private record Envelope(String keyId, byte[] nonce, byte[] ciphertext) {
    }
}
