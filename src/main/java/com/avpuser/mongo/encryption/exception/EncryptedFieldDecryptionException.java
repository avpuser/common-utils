package com.avpuser.mongo.encryption.exception;

/**
 * Thrown when a structurally valid encrypted envelope with a recognized {@code keyId} fails to
 * decrypt: wrong key material for that keyId, corrupted ciphertext, corrupted nonce, or a failed
 * GCM authentication tag check (which also covers AAD/context mismatch, e.g. ciphertext copied
 * from another field or collection).
 */
public class EncryptedFieldDecryptionException extends RuntimeException {

    public EncryptedFieldDecryptionException(String message) {
        super(message);
    }

    public EncryptedFieldDecryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
