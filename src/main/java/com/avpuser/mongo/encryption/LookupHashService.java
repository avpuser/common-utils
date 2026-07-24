package com.avpuser.mongo.encryption;

import com.avpuser.mongo.encryption.exception.PiiEncryptionConfigException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Computes deterministic HMAC-SHA-256 blind-index (lookup) values for {@link Encrypted} fields
 * that declare a {@code lookupField()}, so exact-match search is possible without decrypting the
 * whole collection. Uses a key that is independent from the AES-256 encryption key: an HMAC key
 * must never be reused as an AES key, and vice versa.
 * <p>
 * The field's {@link Encrypted#context()} is mixed into the HMAC input so the same plaintext
 * produces different lookup values for different fields/entities.
 * <p>
 * This class only hashes whatever string it is given - it has no notion of "email", "phone", or
 * any other business concept, and does not normalize its input. Any domain-specific normalization
 * (e.g. trimming/lowercasing an email before it is even assigned to the encrypted field) is the
 * caller's responsibility and belongs in business-layer code, not here.
 */
public final class LookupHashService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private LookupHashService() {
    }

    /**
     * @param value   already-normalized plaintext value to hash (caller is responsible for any
     *                domain-specific normalization before calling this method)
     * @param context stable field context, e.g. {@code "user_v2:contactEmail"}
     * @param lookupKey HMAC-SHA-256 key
     * @return Base64 URL-safe (no padding) encoding of HMAC-SHA-256(lookupKey, context + ":" + value)
     */
    public static String computeLookup(String value, String context, SecretKey lookupKey) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        if (context == null || context.isBlank()) {
            throw new IllegalArgumentException("context must not be blank");
        }
        if (lookupKey == null) {
            throw new IllegalArgumentException("lookupKey must not be null");
        }
        String input = context + ":" + value;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(lookupKey);
            byte[] digest = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new PiiEncryptionConfigException("Unable to compute lookup HMAC: " + e.getMessage(), e);
        }
    }
}
