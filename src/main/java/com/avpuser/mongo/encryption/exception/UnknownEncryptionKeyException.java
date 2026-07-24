package com.avpuser.mongo.encryption.exception;

/**
 * Thrown when a structurally valid encrypted envelope references a {@code keyId} that is not
 * configured on this instance (neither the active key nor any legacy decryption key). This is
 * the expected outcome when, e.g., a production Mongo backup encrypted with the production key
 * is restored locally against a different key set: the field cannot be decrypted here, and the
 * caller must not treat the ciphertext as plaintext, overwrite it, or otherwise touch the record.
 */
public class UnknownEncryptionKeyException extends RuntimeException {

    private final String keyId;

    public UnknownEncryptionKeyException(String message, String keyId) {
        super(message);
        this.keyId = keyId;
    }

    public String getKeyId() {
        return keyId;
    }
}
