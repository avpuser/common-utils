package com.avpuser.mongo.typeconverter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

/**
 * Serializes java.time.Instant as java.util.Date (BSON Date).
 * Ensures full compatibility with MongoDB native date representation.
 */
public class InstantDateSerializer extends StdSerializer<Instant> {

    public InstantDateSerializer() {
        super(Instant.class);
    }

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        // ✅ BSON natively supports java.util.Date with millisecond precision
        gen.writeObject(Date.from(value));
    }
}