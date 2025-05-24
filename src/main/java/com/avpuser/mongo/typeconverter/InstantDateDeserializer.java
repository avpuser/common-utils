package com.avpuser.mongo.typeconverter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

/**
 * Deserializes java.util.Date (from BSON), epoch millis, or ISO-8601 string into java.time.Instant.
 * Supports flexible parsing during migration from older formats.
 */
public class InstantDateDeserializer extends StdDeserializer<Instant> {

    public InstantDateDeserializer() {
        super(Instant.class);
    }

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // BSON → embedded object
        Object embedded = p.getEmbeddedObject();
        if (embedded instanceof Date) {
            return ((Date) embedded).toInstant();
        }
        if (embedded instanceof Long) {
            return Instant.ofEpochMilli((Long) embedded);
        }
        if (embedded instanceof String) {
            return Instant.parse((String) embedded);
        }

        // JSON cases
        switch (p.currentToken()) {
            case VALUE_STRING:
                return Instant.parse(p.getText());
            case VALUE_NUMBER_INT:
                return Instant.ofEpochMilli(p.getLongValue());
            default:
                throw new RuntimeException("Unsupported token for Instant: " + p.currentToken());
        }
    }
}