package com.avpuser.mongo.encryption.jackson;

import com.avpuser.mongo.encryption.PiiEncryptionService;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Decrypts a {@code String} property value on read. Reads the raw scalar value directly (like
 * Jackson's own {@code StringDeserializer} does) rather than delegating to a resolved
 * per-property deserializer: at the point {@link com.fasterxml.jackson.databind.deser.BeanDeserializerModifier}
 * hooks run, individual property deserializers are not resolved yet, so there is nothing to
 * delegate to. The result is run through {@link PiiEncryptionService#decrypt}, which is a no-op
 * for legacy plaintext values and raises a dedicated exception for malformed/foreign-keyed/
 * corrupted envelopes.
 */
final class DecryptingStringDeserializer extends JsonDeserializer<Object> {

    private final PiiEncryptionService encryptionService;
    private final String context;

    DecryptingStringDeserializer(PiiEncryptionService encryptionService, String context) {
        this.encryptionService = encryptionService;
        this.context = context;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String raw = p.getValueAsString();
        if (raw == null) {
            return null;
        }
        return encryptionService.decrypt(raw, context);
    }

    @Override
    public Object getNullValue(DeserializationContext ctxt) {
        return null;
    }
}
