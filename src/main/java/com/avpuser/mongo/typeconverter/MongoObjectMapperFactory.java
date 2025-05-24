package com.avpuser.mongo.typeconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.mongojack.internal.MongoAnnotationIntrospector;

import java.time.Instant;
import java.time.LocalDate;

public class MongoObjectMapperFactory {
    public static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

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