package com.avpuser.mongo.encryption.jackson;

import com.avpuser.mongo.encryption.PiiEncryptionService;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Encrypts a {@code String} property value on write. Installed via
 * {@link com.fasterxml.jackson.databind.ser.BeanPropertyWriter#assignSerializer} so the owning
 * writer's existing null-handling/suppression behavior is left completely untouched: this
 * serializer is only invoked for non-null values.
 */
final class EncryptingStringSerializer extends JsonSerializer<Object> {

    private final PiiEncryptionService encryptionService;
    private final String context;

    EncryptingStringSerializer(PiiEncryptionService encryptionService, String context) {
        this.encryptionService = encryptionService;
        this.context = context;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(encryptionService.encrypt((String) value, context));
    }
}
