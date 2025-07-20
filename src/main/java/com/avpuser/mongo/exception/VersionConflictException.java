package com.avpuser.mongo.exception;

public class VersionConflictException extends RuntimeException {

    public VersionConflictException(String message) {
        super(message);
    }

    public VersionConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    public VersionConflictException(Throwable cause) {
        super(cause);
    }

    public VersionConflictException() {
        super("Version conflict: the document was modified by another process.");
    }
}