package com.avpuser.mongo.typeconverter;

import com.avpuser.mongo.encryption.PiiEncryptionService;
import com.avpuser.mongo.encryption.jackson.PiiEncryptionModule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.mongojack.internal.MongoAnnotationIntrospector;

import java.time.Instant;
import java.time.LocalDate;

public class MongoObjectMapperFactory {

    /** Without field-level PII encryption. Use {@link #createObjectMapper(PiiEncryptionService)} for entities with {@code @Encrypted} fields. */
    public static ObjectMapper createObjectMapper() {
        return createObjectMapper(null);
    }

    public static ObjectMapper createObjectMapper(PiiEncryptionService encryptionService) {
        ObjectMapper mapper = new ObjectMapper();

        // Let RuntimeExceptions raised by custom (de)serializers - notably the PII decryption
        // exceptions below - propagate to the caller unwrapped, instead of being repackaged into
        // a generic JsonMappingException. Callers that need to distinguish "unknown key" from
        // "corrupted payload" from "auth failure" need the real exception type.
        mapper.disable(DeserializationFeature.WRAP_EXCEPTIONS);

        if (encryptionService != null) {
            mapper.registerModule(new PiiEncryptionModule(encryptionService));
        }

        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // Custom serialization/deserialization for Instant
        javaTimeModule.addSerializer(Instant.class, new InstantDateSerializer());
        javaTimeModule.addDeserializer(Instant.class, new InstantDateDeserializer());

        mapper.registerModule(javaTimeModule);

        // Custom serialization/deserialization for LocalDate using epochDay (long)
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDate.class, new LocalDateEpochDaySerializer());
        module.addDeserializer(LocalDate.class, new LocalDateEpochDayDeserializer());
        mapper.registerModule(module);

        // Prevents writing java.time types as timestamps
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Enables support for Mongo-specific annotations (e.g. @Id)
        MongoAnnotationIntrospector mongoIntrospector =
                new MongoAnnotationIntrospector(TypeFactory.defaultInstance());
        mapper.setAnnotationIntrospector(
                AnnotationIntrospectorPair.pair(mongoIntrospector, mapper.getSerializationConfig().getAnnotationIntrospector())
        );

        return mapper;
    }
}