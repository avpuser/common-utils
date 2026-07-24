package com.avpuser.mongo.encryption;

import com.avpuser.mongo.encryption.exception.PiiEncryptionConfigException;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflection-based discovery of {@link Encrypted} fields on an entity class, with eager
 * validation (blank context, non-String field, missing/wrong-typed lookup field) and per-class
 * caching. Used by the Jackson serializer/deserializer modifiers so entity classes without any
 * {@link Encrypted} field pay no cost beyond one cache lookup.
 */
public final class EncryptedFieldIntrospector {

    private static final Map<Class<?>, Map<String, Encrypted>> ENCRYPTED_FIELDS_CACHE = new ConcurrentHashMap<>();

    private EncryptedFieldIntrospector() {
    }

    /** Returns {@code @Encrypted} fields declared on {@code beanClass} (and its superclasses), by field name. */
    public static Map<String, Encrypted> scanEncryptedFields(Class<?> beanClass) {
        return ENCRYPTED_FIELDS_CACHE.computeIfAbsent(beanClass, EncryptedFieldIntrospector::doScan);
    }

    private static Map<String, Encrypted> doScan(Class<?> beanClass) {
        Map<String, Encrypted> result = new LinkedHashMap<>();
        for (Class<?> c = beanClass; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                Encrypted annotation = field.getAnnotation(Encrypted.class);
                if (annotation != null) {
                    validate(beanClass, field, annotation);
                    result.put(field.getName(), annotation);
                }
            }
        }
        return Map.copyOf(result);
    }

    private static void validate(Class<?> beanClass, Field field, Encrypted annotation) {
        if (!field.getType().equals(String.class)) {
            throw new PiiEncryptionConfigException("@Encrypted field " + beanClass.getName() + "#"
                    + field.getName() + " must be of type String, but was " + field.getType());
        }
        if (annotation.context() == null || annotation.context().isBlank()) {
            throw new PiiEncryptionConfigException("@Encrypted field " + beanClass.getName() + "#"
                    + field.getName() + " must declare a non-blank context()");
        }
        String lookupFieldName = annotation.lookupField();
        if (!lookupFieldName.isBlank()) {
            if (lookupFieldName.equals(field.getName())) {
                throw new PiiEncryptionConfigException("@Encrypted field " + beanClass.getName() + "#"
                        + field.getName() + " cannot declare itself as its own lookupField()");
            }
            Field lookupField = findField(beanClass, lookupFieldName);
            if (lookupField == null) {
                throw new PiiEncryptionConfigException("@Encrypted field " + beanClass.getName() + "#"
                        + field.getName() + " declares lookupField='" + lookupFieldName
                        + "' but no such field exists on " + beanClass.getName());
            }
            if (!lookupField.getType().equals(String.class)) {
                throw new PiiEncryptionConfigException("@Encrypted lookupField '" + lookupFieldName
                        + "' on " + beanClass.getName() + " must be of type String, but was " + lookupField.getType());
            }
        }
    }

    /** Finds a declared field by name on {@code beanClass} or a superclass, made accessible. Returns {@code null} if absent. */
    public static Field findField(Class<?> beanClass, String name) {
        for (Class<?> c = beanClass; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                // try superclass
            }
        }
        return null;
    }
}
