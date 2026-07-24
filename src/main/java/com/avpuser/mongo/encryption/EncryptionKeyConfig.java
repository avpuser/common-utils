package com.avpuser.mongo.encryption;

import com.avpuser.mongo.encryption.exception.PiiEncryptionConfigException;

import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validated, immutable key material for {@link PiiEncryptionService}: one active AES-256 key used
 * for all new encryption, an arbitrary number of additional AES-256 keys kept only to decrypt
 * older data (key rotation), and one HMAC-SHA-256 key used for blind-index lookup values.
 * <p>
 * Construction fails fast ({@link PiiEncryptionConfigException}) on any misconfiguration: missing
 * or blank keys, invalid Base64, wrong AES key length, too-short HMAC key, an active key id with
 * no corresponding key material, or an invalid key id format. This is intentional: the
 * application must not start with broken encryption configuration.
 */
public final class EncryptionKeyConfig {

    /** AES-256 key length in bytes. */
    private static final int AES_KEY_BYTES = 32;

    /** Minimum acceptable HMAC-SHA-256 key length in bytes (NIST SP 800-107 recommendation). */
    private static final int MIN_HMAC_KEY_BYTES = 32;

    /** keyId is embedded in the envelope string split on ':', so it must not contain that char. */
    private static final Pattern KEY_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");

    private final String activeKeyId;
    private final Map<String, SecretKeySpec> aesKeysById;
    private final SecretKeySpec lookupKey;

    private EncryptionKeyConfig(String activeKeyId, Map<String, SecretKeySpec> aesKeysById, SecretKeySpec lookupKey) {
        this.activeKeyId = activeKeyId;
        this.aesKeysById = aesKeysById;
        this.lookupKey = lookupKey;
    }

    /**
     * @param activeKeyId               id of the key used to encrypt new values; must be present in
     *                                  either {@code activeKeyBase64} (via activeKeyId/activeKeyBase64 pair)
     *                                  or {@code legacyKeysBase64ById}
     * @param activeKeyBase64           Base64-encoded 32-byte AES-256 key used for new encryption
     * @param legacyKeysBase64ById      additional Base64-encoded AES-256 keys, by keyId, kept only for
     *                                  decrypting data encrypted by a previously active key; may be empty
     * @param lookupKeyBase64           Base64-encoded HMAC-SHA-256 key (at least 32 bytes decoded)
     */
    public static EncryptionKeyConfig create(String activeKeyId,
                                              String activeKeyBase64,
                                              Map<String, String> legacyKeysBase64ById,
                                              String lookupKeyBase64) {
        if (isBlank(activeKeyId)) {
            throw new PiiEncryptionConfigException("PII encryption active key id is missing/blank");
        }
        if (!KEY_ID_PATTERN.matcher(activeKeyId).matches()) {
            throw new PiiEncryptionConfigException("PII encryption active key id has invalid format: " + activeKeyId);
        }

        Map<String, SecretKeySpec> aesKeys = new LinkedHashMap<>();
        aesKeys.put(activeKeyId, decodeAesKey(activeKeyId, activeKeyBase64));

        if (legacyKeysBase64ById != null) {
            for (Map.Entry<String, String> entry : legacyKeysBase64ById.entrySet()) {
                String keyId = entry.getKey();
                if (isBlank(keyId)) {
                    throw new PiiEncryptionConfigException("PII encryption legacy key id is blank");
                }
                if (!KEY_ID_PATTERN.matcher(keyId).matches()) {
                    throw new PiiEncryptionConfigException("PII encryption legacy key id has invalid format: " + keyId);
                }
                if (aesKeys.containsKey(keyId)) {
                    throw new PiiEncryptionConfigException("Duplicate PII encryption key id: " + keyId);
                }
                aesKeys.put(keyId, decodeAesKey(keyId, entry.getValue()));
            }
        }

        SecretKeySpec lookupKey = decodeLookupKey(lookupKeyBase64);

        return new EncryptionKeyConfig(activeKeyId, Map.copyOf(aesKeys), lookupKey);
    }

    /** Generates a fresh, valid-length random key for use as an AES-256 key, Base64-encoded. Test helper. */
    public static String generateRandomAesKeyBase64() {
        return generateRandomKeyBase64(AES_KEY_BYTES);
    }

    /** Generates a fresh, valid-length random key for use as an HMAC-SHA-256 key, Base64-encoded. Test helper. */
    public static String generateRandomHmacKeyBase64() {
        return generateRandomKeyBase64(MIN_HMAC_KEY_BYTES);
    }

    private static String generateRandomKeyBase64(int lengthBytes) {
        byte[] bytes = new byte[lengthBytes];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static SecretKeySpec decodeAesKey(String keyId, String base64Key) {
        byte[] bytes = decodeBase64("PII encryption key '" + keyId + "'", base64Key);
        if (bytes.length != AES_KEY_BYTES) {
            throw new PiiEncryptionConfigException(
                    "PII encryption key '" + keyId + "' must decode to " + AES_KEY_BYTES
                            + " bytes (AES-256), but was " + bytes.length + " bytes");
        }
        return new SecretKeySpec(bytes, "AES");
    }

    private static SecretKeySpec decodeLookupKey(String base64Key) {
        byte[] bytes = decodeBase64("PII lookup key", base64Key);
        if (bytes.length < MIN_HMAC_KEY_BYTES) {
            throw new PiiEncryptionConfigException(
                    "PII lookup key must decode to at least " + MIN_HMAC_KEY_BYTES
                            + " bytes (HMAC-SHA-256), but was " + bytes.length + " bytes");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    private static byte[] decodeBase64(String label, String base64Value) {
        if (isBlank(base64Value)) {
            throw new PiiEncryptionConfigException(label + " is missing/blank");
        }
        try {
            return Base64.getDecoder().decode(base64Value.trim());
        } catch (IllegalArgumentException e) {
            throw new PiiEncryptionConfigException(label + " is not valid Base64", e);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public String getActiveKeyId() {
        return activeKeyId;
    }

    public SecretKeySpec getActiveKey() {
        return aesKeysById.get(activeKeyId);
    }

    /** Returns the AES key for the given keyId, or {@code null} if not configured. */
    public SecretKeySpec getKeyById(String keyId) {
        return aesKeysById.get(keyId);
    }

    public boolean hasKey(String keyId) {
        return aesKeysById.containsKey(keyId);
    }

    public SecretKeySpec getLookupKey() {
        return lookupKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EncryptionKeyConfig that)) return false;
        return activeKeyId.equals(that.activeKeyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(activeKeyId);
    }
}
