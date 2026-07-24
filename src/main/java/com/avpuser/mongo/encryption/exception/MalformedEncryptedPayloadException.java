package com.avpuser.mongo.encryption.exception;

/**
 * Thrown when a value carries the encrypted-envelope prefix (e.g. {@code "msenc:"}) but its
 * structure is invalid (wrong number of segments, unsupported version, invalid Base64, wrong
 * nonce length, ciphertext too short to contain an auth tag). Never thrown for values that don't
 * look like an envelope at all - those are treated as legacy plaintext. Never silently treated as
 * plaintext or as valid ciphertext: this always surfaces as a controlled error so corrupted data
 * cannot be mistaken for a decryptable or a readable value.
 */
public class MalformedEncryptedPayloadException extends RuntimeException {

    public MalformedEncryptedPayloadException(String message) {
        super(message);
    }

    public MalformedEncryptedPayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
