package com.avpuser.mongo.typeconverter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.LocalDate;

// Serializes LocalDate as long epochDay (number of days since 1970-01-01).
public class LocalDateEpochDaySerializer extends StdSerializer<LocalDate> {
    public LocalDateEpochDaySerializer() {
        super(LocalDate.class);
    }

    @Override
    public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeNumber(value.toEpochDay()); // includes pre-1970 dates
    }
}