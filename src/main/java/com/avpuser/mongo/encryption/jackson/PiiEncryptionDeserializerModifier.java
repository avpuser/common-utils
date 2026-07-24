package com.avpuser.mongo.encryption.jackson;

import com.avpuser.mongo.encryption.Encrypted;
import com.avpuser.mongo.encryption.EncryptedFieldIntrospector;
import com.avpuser.mongo.encryption.PiiEncryptionService;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;

import java.util.Map;

/**
 * For entity classes with one or more {@link Encrypted} fields, wraps the deserializer for each
 * such field so the stored envelope (or legacy plaintext) is transparently decrypted back to
 * plaintext when the entity is read. Classes without any {@link Encrypted} field are returned
 * completely untouched.
 */
public final class PiiEncryptionDeserializerModifier extends BeanDeserializerModifier {

    private final PiiEncryptionService encryptionService;

    public PiiEncryptionDeserializerModifier(PiiEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public BeanDeserializerBuilder updateBuilder(DeserializationConfig config, BeanDescription beanDesc,
                                                  BeanDeserializerBuilder builder) {
        Map<String, Encrypted> encryptedFields = EncryptedFieldIntrospector.scanEncryptedFields(beanDesc.getBeanClass());
        if (encryptedFields.isEmpty()) {
            return builder;
        }

        for (Map.Entry<String, Encrypted> entry : encryptedFields.entrySet()) {
            SettableBeanProperty original = builder.findProperty(new PropertyName(entry.getKey()));
            if (original == null) {
                continue;
            }
            SettableBeanProperty replaced = original.withValueDeserializer(
                    new DecryptingStringDeserializer(encryptionService, entry.getValue().context()));
            builder.addOrReplaceProperty(replaced, true);
        }
        return builder;
    }
}
