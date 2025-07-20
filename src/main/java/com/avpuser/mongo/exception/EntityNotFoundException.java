package com.avpuser.mongo.exception;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String entityName, String id) {
        super("No " + entityName + " found with id: " + id);
    }

    public EntityNotFoundException(String message) {
        super(message);
    }
}