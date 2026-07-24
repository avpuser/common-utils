package com.avpuser.mongo.encryption;

import com.avpuser.mongo.encryption.exception.PiiEncryptionConfigException;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionKeyConfigTest {

    private static final String VALID_AES_KEY = EncryptionKeyConfig.generateRandomAesKeyBase64();
    private static final String VALID_HMAC_KEY = EncryptionKeyConfig.generateRandomHmacKeyBase64();

    @Test
    void validConfig_loadsSuccessfully() {
        EncryptionKeyConfig config = EncryptionKeyConfig.create("pii-v1", VALID_AES_KEY, Map.of(), VALID_HMAC_KEY);
        assertEquals("pii-v1", config.getActiveKeyId());
        assertNotNull(config.getActiveKey());
        assertNotNull(config.getLookupKey());
        assertTrue(config.hasKey("pii-v1"));
    }

    @Test
    void missingActiveKeyId_throws() {
        assertThrows(PiiEncryptionConfigException.class,
                () -> EncryptionKeyConfig.create(null, VALID_AES_KEY, Map.of(), VALID_HMAC_KEY));
        assertThrows(PiiEncryptionConfigException.class,
                () -> EncryptionKeyConfig.create("  ", VALID_AES_KEY, Map.of(), VALID_HMAC_KEY));
    }

    @Test
    void invalidActiveKeyIdFormat_throws() {
        assertThrows(PiiEncryptionConfigException.class,
                () -> EncryptionKeyConfig.create("bad:id", VALID_AES_KEY, Map.of(), VALID_HMAC_KEY));
    }

    @Test
    void missingEncryptionKey_throws() {
        assertThrows(PiiEncryptionConfigException.class,
                () -> EncryptionKeyConfig.create("pii-v1", null, Map.of(), VALID_HMAC_KEY));
        assertThrows(PiiEncryptionConfigException.class,
                () -> EncryptionKeyConfig.create("pii-v1", "   ", Map.of(), VALID_HMAC_KEY));
    }

    @Test
    void invalidBase64EncryptionKey_throws() {
        assertThrows(PiiEncryptionConfigException.class,
                () -> EncryptionKeyConfig.create("pii-v1", "not-valid-base64-!!!", Map.of(), VALID_HMAC_KEY));
    }

    @Test
    void wrongLengthAesKey_throws() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]); // AES-128 length, not 256
        assertThrows(PiiEncryptionConfigException.class,
                () -> EncryptionKeyConfig.create("pii-v1", shortKey, Map.of(), VALID_HMAC_KEY));
    }

    @Test
    void missingLookupKey_throws() {
        assertThrows(PiiEncryptionConfigException.class,
                () -> EncryptionKeyConfig.create("pii-v1", VALID_AES_KEY, Map.of(), null));
    }

    @Test
    void tooShortLookupKey_throws() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThrows(PiiEncryptionConfigException.class,
                () -> EncryptionKeyConfig.create("pii-v1", VALID_AES_KEY, Map.of(), shortKey));
    }

    @Test
    void invalidBase64LookupKey_throws() {
        assertThrows(PiiEncryptionConfigException.class,
                () -> EncryptionKeyConfig.create("pii-v1", VALID_AES_KEY, Map.of(), "!!!not-base64!!!"));
    }

    @Test
    void duplicateKeyIds_throws() {
        Map<String, String> legacy = Map.of("pii-v1", VALID_AES_KEY);
        assertThrows(PiiEncryptionConfigException.class,
                () -> EncryptionKeyConfig.create("pii-v1", VALID_AES_KEY, legacy, VALID_HMAC_KEY));
    }

    @Test
    void blankLegacyKeyId_throws() {
        Map<String, String> legacy = new java.util.HashMap<>();
        legacy.put("", VALID_AES_KEY);
        assertThrows(PiiEncryptionConfigException.class,
                () -> EncryptionKeyConfig.create("pii-v1", VALID_AES_KEY, legacy, VALID_HMAC_KEY));
    }

    @Test
    void legacyKeyIsAvailableForDecryption_butIsNotTheActiveKey() {
        String legacyKeyMaterial = EncryptionKeyConfig.generateRandomAesKeyBase64();
        EncryptionKeyConfig config = EncryptionKeyConfig.create(
                "pii-v2", VALID_AES_KEY, Map.of("pii-v1-legacy", legacyKeyMaterial), VALID_HMAC_KEY);

        assertEquals("pii-v2", config.getActiveKeyId());
        assertTrue(config.hasKey("pii-v1-legacy"));
        assertNotEquals(config.getKeyById("pii-v1-legacy"), config.getActiveKey());
    }

    @Test
    void unknownKeyId_returnsNull() {
        EncryptionKeyConfig config = EncryptionKeyConfig.create("pii-v1", VALID_AES_KEY, Map.of(), VALID_HMAC_KEY);
        assertNull(config.getKeyById("does-not-exist"));
        assertFalse(config.hasKey("does-not-exist"));
    }
}
