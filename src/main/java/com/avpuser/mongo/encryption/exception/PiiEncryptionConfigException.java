package com.avpuser.mongo.encryption.exception;

/**
 * Thrown at startup when the PII encryption/lookup key configuration is missing or invalid.
 * Intentionally a fail-fast, unchecked exception: the application must not start with broken
 * encryption configuration in any environment.
 */
public class PiiEncryptionConfigException extends RuntimeException {

    public PiiEncryptionConfigException(String message) {
        super(message);
    }

    public PiiEncryptionConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
