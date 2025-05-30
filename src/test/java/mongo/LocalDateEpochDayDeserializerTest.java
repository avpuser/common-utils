package mongo;

import com.avpuser.mongo.typeconverter.LocalDateEpochDayDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalDateEpochDayDeserializerTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(LocalDate.class, new LocalDateEpochDayDeserializer());
        mapper.registerModule(module);
    }

    @Test
    void shouldDeserializeFromEpochDay() throws IOException {
        // 1970-01-01 -> epochDay = 0
        String json = "0";
        LocalDate result = mapper.readValue(json, LocalDate.class);
        assertEquals(LocalDate.of(1970, 1, 1), result);
    }

    @Test
    void shouldDeserializeFromPositiveEpochDay() throws IOException {
        // 1981-11-08 -> epochDay = 4355
        long epochDay = LocalDate.of(1981, 11, 8).toEpochDay();
        String json = String.valueOf(epochDay);
        LocalDate result = mapper.readValue(json, LocalDate.class);
        assertEquals(LocalDate.of(1981, 11, 8), result);
    }

    @Test
    void shouldDeserializeFromNegativeEpochDay() throws IOException {
        // 1900-01-01 -> epochDay = -25567
        long epochDay = LocalDate.of(1900, 1, 1).toEpochDay();
        String json = String.valueOf(epochDay);
        LocalDate result = mapper.readValue(json, LocalDate.class);
        assertEquals(LocalDate.of(1900, 1, 1), result);
    }

    @Test
    void shouldFailOnInvalidType() {
        String json = "\"not-a-number\"";
        assertThrows(DateTimeParseException.class, () -> mapper.readValue(json, LocalDate.class));
    }
}