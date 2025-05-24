package com.avpuser.mongo.typeconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.mongojack.internal.MongoAnnotationIntrospector;

import java.time.Instant;

public class ObjectMapperFactory {
    public static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // 👇 Кастомная обработка Instant
        javaTimeModule.addSerializer(Instant.class, new InstantDateSerializer());
        javaTimeModule.addDeserializer(Instant.class, new InstantDateDeserializer());

        mapper.registerModule(javaTimeModule);

        // 👇 Не влияет на Instant напрямую, но оставляем на всякий случай
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // ✅ Настраиваем MongoAnnotationIntrospector
        MongoAnnotationIntrospector mongoIntrospector =
                new MongoAnnotationIntrospector(TypeFactory.defaultInstance());
        mapper.setAnnotationIntrospector(
                AnnotationIntrospectorPair.pair(mongoIntrospector, mapper.getSerializationConfig().getAnnotationIntrospector())
        );

        return mapper;
    }
}