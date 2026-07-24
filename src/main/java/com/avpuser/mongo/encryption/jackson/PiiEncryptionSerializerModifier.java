package com.avpuser.mongo.encryption.jackson;

import com.avpuser.mongo.encryption.Encrypted;
import com.avpuser.mongo.encryption.EncryptedFieldIntrospector;
import com.avpuser.mongo.encryption.PiiEncryptionService;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * For entity classes with one or more {@link Encrypted} fields, rewires their serialization so
 * that the encrypted field's plaintext is replaced by an encrypted envelope on write, and any
 * declared lookup field is recomputed from that same plaintext. Classes without any
 * {@link Encrypted} field are returned completely untouched.
 */
public final class PiiEncryptionSerializerModifier extends BeanSerializerModifier {

    private final PiiEncryptionService encryptionService;

    public PiiEncryptionSerializerModifier(PiiEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
                                                       List<BeanPropertyWriter> beanProperties) {
        Map<String, Encrypted> encryptedFields = EncryptedFieldIntrospector.scanEncryptedFields(beanDesc.getBeanClass());
        if (encryptedFields.isEmpty()) {
            return beanProperties;
        }

        Map<String, String> contextByLookupFieldName = new LinkedHashMap<>();
        Map<String, Field> sourceFieldByLookupFieldName = new LinkedHashMap<>();
        for (Map.Entry<String, Encrypted> entry : encryptedFields.entrySet()) {
            String sourceFieldName = entry.getKey();
            Encrypted annotation = entry.getValue();
            if (!annotation.lookupField().isBlank()) {
                contextByLookupFieldName.put(annotation.lookupField(), annotation.context());
                sourceFieldByLookupFieldName.put(annotation.lookupField(),
                        EncryptedFieldIntrospector.findField(beanDesc.getBeanClass(), sourceFieldName));
            }
        }

        List<BeanPropertyWriter> result = new ArrayList<>(beanProperties.size());
        for (BeanPropertyWriter writer : beanProperties) {
            Encrypted encryptedAnnotation = encryptedFields.get(writer.getName());
            if (encryptedAnnotation != null) {
                writer.assignSerializer(new EncryptingStringSerializer(encryptionService, encryptedAnnotation.context()));
                result.add(writer);
                continue;
            }
            String lookupContext = contextByLookupFieldName.get(writer.getName());
            if (lookupContext != null) {
                Field sourceField = sourceFieldByLookupFieldName.get(writer.getName());
                result.add(new LookupIndexBeanPropertyWriter(writer, sourceField, lookupContext, encryptionService));
                continue;
            }
            result.add(writer);
        }
        return result;
    }
}
