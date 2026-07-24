package com.avpuser.mongo.encryption.jackson;

import com.avpuser.mongo.encryption.PiiEncryptionService;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;

import java.lang.reflect.Field;

/**
 * Replaces the writer for a blind-index (lookup) field so it always recomputes its value from the
 * still-plaintext, in-memory source {@link Encrypted} field, ignoring whatever the lookup field
 * itself currently holds. This is what makes an encrypted field and its lookup field update
 * atomically as part of the same document write: there is no separate step where business code
 * could forget to refresh the lookup value, and no way for it to drift out of sync with the source
 * field. This class hashes whatever plaintext the source field holds - it has no notion of what
 * that field represents (email, phone, etc.); any normalization of that plaintext is the caller's
 * responsibility, applied before the value is ever assigned to the source field.
 */
final class LookupIndexBeanPropertyWriter extends BeanPropertyWriter {

    private final Field sourceField;
    private final String context;
    private final PiiEncryptionService encryptionService;

    LookupIndexBeanPropertyWriter(BeanPropertyWriter base, Field sourceField, String context,
                                   PiiEncryptionService encryptionService) {
        super(base);
        this.sourceField = sourceField;
        this.context = context;
        this.encryptionService = encryptionService;
    }

    @Override
    public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
        String plaintext = (String) sourceField.get(bean);
        String lookup = (plaintext == null || plaintext.isBlank())
                ? null
                : encryptionService.computeLookup(plaintext, context);

        gen.writeFieldName(getName());
        if (lookup == null) {
            gen.writeNull();
        } else {
            gen.writeString(lookup);
        }
    }
}
