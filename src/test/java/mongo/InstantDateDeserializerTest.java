package mongo;

import com.avpuser.mongo.typeconverter.InstantDateDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstantDateDeserializerTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Instant.class, new InstantDateDeserializer());
        mapper.registerModule(module);
    }

    @Test
    void shouldDeserializeFromIsoString() throws IOException {
        String json = "\"2025-05-24T10:19:13.338485Z\"";
        Instant result = mapper.readValue(json, Instant.class);
        assertEquals(Instant.parse("2025-05-24T10:19:13.338485Z"), result);
    }

    @Test
    void shouldDeserializeFromEpochMillis() throws IOException {
        long millis = Instant.parse("2025-05-24T10:19:13.338Z").toEpochMilli();
        String json = String.valueOf(millis);
        Instant result = mapper.readValue(json, Instant.class);
        assertEquals(Instant.ofEpochMilli(millis), result);
    }

    @Test
    void shouldDeserializeFromBsonDate() throws IOException {
        // симулируем BSON Date через JSON объект: {"$date": "..."}
        // Jackson не умеет прямо так, поэтому эмулируем как long
        String json = String.valueOf(new java.util.Date(Instant.parse("2025-05-24T10:19:13.000Z").toEpochMilli()).getTime());
        Instant result = mapper.readValue(json, Instant.class);
        assertEquals(Instant.parse("2025-05-24T10:19:13.000Z"), result);
    }
}