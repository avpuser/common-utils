package utils;

import com.avpuser.test.MockTest;
import com.avpuser.utils.JsonUtils;
import com.avpuser.utils.LlmJsonRepair;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParseException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@MockTest
public class LlmJsonRepairTest {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LabReportDto {
        public List<LabTestDto> tests;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LabTestDto {
        public String name;
        public String value;
        public String unit;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SimpleDto {
        public String name;
        public String note;
    }

    @Test
    void validJson_unchanged_andDeserializes() {
        String json = "{\"name\":\"Гемоглобин\",\"note\":\"ok\\\\path\"}";
        LlmJsonRepair.RepairResult repaired = LlmJsonRepair.repairInvalidEscapes(json);
        assertEquals(0, repaired.fixedEscapes());
        assertEquals(json, repaired.json());

        SimpleDto dto = LlmJsonRepair.deserializeWithEscapeRepair(json, SimpleDto.class, "TestParser");
        assertEquals("Гемоглобин", dto.name);
        assertEquals("ok\\path", dto.note);
    }

    @Test
    void invalidNeutrophilEscape_repairedPreservingBackslash_andAllTests() {
        // LLM copied OCR "П\Я" / "с\я" without JSON-escaping the backslash.
        String invalidJson = "{\"tests\":["
                + "{\"name\":\"Нейтрофилы П\\" + "Я+СЯ\",\"value\":\"1.2\",\"unit\":\"10^9/л\"},"
                + "{\"name\":\"Нейтрофилы (с\\" + "я + п\\" + "я), %\",\"value\":\"55\",\"unit\":\"%\"},"
                + "{\"name\":\"Гемоглобин\",\"value\":\"140\",\"unit\":\"г/л\"}"
                + "]}";

        assertThrows(RuntimeException.class,
                () -> JsonUtils.deserializeJsonToObject(invalidJson, LabReportDto.class));

        LabReportDto report = LlmJsonRepair.deserializeWithEscapeRepair(
                invalidJson, LabReportDto.class, "TestsRawLabReportParser");

        assertNotNull(report.tests);
        assertEquals(3, report.tests.size(), "all tests must be preserved after escape repair");
        assertEquals("Нейтрофилы П\\Я+СЯ", report.tests.get(0).name);
        assertEquals("1.2", report.tests.get(0).value);
        assertEquals("10^9/л", report.tests.get(0).unit);
        assertEquals("Нейтрофилы (с\\я + п\\я), %", report.tests.get(1).name);
        assertEquals("55", report.tests.get(1).value);
        assertEquals("Гемоглобин", report.tests.get(2).name);
        assertEquals("140", report.tests.get(2).value);
    }

    @Test
    void repairInvalidEscapes_countsSingleFix() {
        String json = "{\"name\":\"П\\" + "Я\"}";
        LlmJsonRepair.RepairResult repaired = LlmJsonRepair.repairInvalidEscapes(json);
        assertEquals(1, repaired.fixedEscapes());
        assertEquals("{\"name\":\"П\\\\Я\"}", repaired.json());
    }

    @Test
    void validEscapes_backslashQuoteNewlineUnicode_untouched() {
        String json = "{\"name\":\"A\\\\B\",\"note\":\"say \\\"hi\\\"\\nand \\u0410\"}";
        LlmJsonRepair.RepairResult repaired = LlmJsonRepair.repairInvalidEscapes(json);
        assertEquals(0, repaired.fixedEscapes());
        assertEquals(json, repaired.json());

        SimpleDto dto = LlmJsonRepair.deserializeWithEscapeRepair(json, SimpleDto.class, "TestParser");
        assertEquals("A\\B", dto.name);
        assertEquals("say \"hi\"\nand А", dto.note);
    }

    @Test
    void multipleInvalidEscapes_allFixed() {
        String json = "{\"name\":\"с\\" + "я + п\\" + "я / П\\" + "Я\"}";
        LlmJsonRepair.RepairResult repaired = LlmJsonRepair.repairInvalidEscapes(json);
        assertEquals(3, repaired.fixedEscapes());

        SimpleDto dto = LlmJsonRepair.deserializeWithEscapeRepair(json, SimpleDto.class, "TestParser");
        assertEquals("с\\я + п\\я / П\\Я", dto.name);
    }

    @Test
    void escapesOutsideStrings_notRewritten() {
        String json = "{\"name\":oops\\" + "Я}";
        LlmJsonRepair.RepairResult repaired = LlmJsonRepair.repairInvalidEscapes(json);
        assertEquals(0, repaired.fixedEscapes());
        assertEquals(json, repaired.json());
    }

    @Test
    void trailingLoneBackslash_notRepaired_remainsUnrecoverable() {
        // Truncated mid-escape: {"name":"abc\
        String truncated = "{\"name\":\"abc\\";
        LlmJsonRepair.RepairResult repaired = LlmJsonRepair.repairInvalidEscapes(truncated);
        assertEquals(0, repaired.fixedEscapes());
        assertEquals(truncated, repaired.json());

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> LlmJsonRepair.deserializeWithEscapeRepair(truncated, SimpleDto.class, "TestParser"));
        assertNotNull(findParse(thrown));
        assertEquals(0, thrown.getSuppressed().length);
    }

    @Test
    void truncatedJson_stillFails() {
        String truncated = "{\"tests\":[{\"name\":\"Гемоглобин\",\"value\":\"140\"";
        assertThrows(RuntimeException.class,
                () -> LlmJsonRepair.deserializeWithEscapeRepair(truncated, LabReportDto.class, "TestParser"));
    }

    @Test
    void missingBrace_stillFails() {
        String broken = "{\"tests\":[{\"name\":\"X\"}";
        assertThrows(RuntimeException.class,
                () -> LlmJsonRepair.deserializeWithEscapeRepair(broken, LabReportDto.class, "TestParser"));
    }

    @Test
    void repairThenStillUnparseable_preservesOriginalAndSuppressesSecond() {
        // Invalid escape is repaired, but JSON remains structurally truncated → second parse fails.
        String json = "{\"name\":\"П\\" + "Я\"";
        LlmJsonRepair.RepairResult repaired = LlmJsonRepair.repairInvalidEscapes(json);
        assertEquals(1, repaired.fixedEscapes());

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> LlmJsonRepair.deserializeWithEscapeRepair(json, SimpleDto.class, "TestParser"));

        assertNotNull(findParse(thrown), "original JsonParseException must remain the primary failure");
        assertEquals(1, thrown.getSuppressed().length);
        assertInstanceOf(RuntimeException.class, thrown.getSuppressed()[0]);
        assertTrue(thrown.getMessage() != null && thrown.getMessage().contains("Error parsing JSON"));
    }

    @Test
    void markdownFencedInvalidEscape_repaired() {
        String fenced = "```json\n{\"name\":\"П\\" + "Я\"}\n```";
        SimpleDto dto = LlmJsonRepair.deserializeWithEscapeRepair(fenced, SimpleDto.class, "TestParser");
        assertEquals("П\\Я", dto.name);
    }

    @Test
    void alreadyValidDoubleBackslash_neutrophils_unchanged() {
        String valid = "{\"name\":\"Нейтрофилы П\\\\Я+СЯ\"}";
        LlmJsonRepair.RepairResult repaired = LlmJsonRepair.repairInvalidEscapes(valid);
        assertEquals(0, repaired.fixedEscapes());

        SimpleDto dto = LlmJsonRepair.deserializeWithEscapeRepair(valid, SimpleDto.class, "TestParser");
        assertEquals("Нейтрофилы П\\Я+СЯ", dto.name);
    }

    @Test
    void cachedInvalidLlmResponse_succeedsAfterRepair() {
        // Same payload that CacheAiExecutor would return on reprocess.
        String cachedInvalid = "{\"tests\":[{\"name\":\"Нейтрофилы П\\" + "Я+СЯ\",\"value\":\"2.1\"}]}";
        LabReportDto report = LlmJsonRepair.deserializeWithEscapeRepair(
                cachedInvalid, LabReportDto.class, "TestsRawLabReportParser");
        assertEquals(1, report.tests.size());
        assertEquals("Нейтрофилы П\\Я+СЯ", report.tests.get(0).name);
        assertEquals("2.1", report.tests.get(0).value);
    }

    @Test
    void errorTypeConstant_isFixedInvalidEscape_noMedicalContent() {
        assertEquals("INVALID_ESCAPE", LlmJsonRepair.ERROR_TYPE_INVALID_ESCAPE);
        assertFalse(LlmJsonRepair.ERROR_TYPE_INVALID_ESCAPE.contains("Я"));
        assertFalse(LlmJsonRepair.ERROR_TYPE_INVALID_ESCAPE.toLowerCase().contains("нейтрофил"));
    }

    private static JsonParseException findParse(Throwable t) {
        while (t != null) {
            if (t instanceof JsonParseException jpe) {
                return jpe;
            }
            t = t.getCause();
        }
        return null;
    }
}
