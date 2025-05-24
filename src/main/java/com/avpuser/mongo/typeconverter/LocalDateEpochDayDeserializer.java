package com.avpuser.mongo.typeconverter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalDate;

public class LocalDateEpochDayDeserializer extends StdDeserializer<LocalDate> {
    public LocalDateEpochDayDeserializer() {
        super(LocalDate.class);
    }

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        switch (p.currentToken()) {
            case VALUE_NUMBER_INT:
                return LocalDate.ofEpochDay(p.getLongValue());
            case VALUE_STRING:
                return LocalDate.parse(p.getText()); // fallback for existing "yyyy-MM-dd" values
            default:
                throw new RuntimeException("Unsupported format for LocalDate: " + p.currentToken());
        }
    }
}