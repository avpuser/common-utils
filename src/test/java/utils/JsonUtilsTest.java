package utils;

import com.avpuser.test.MockTest;
import com.avpuser.utils.JsonUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
@MockTest

public class JsonUtilsTest {

    public static class TestObject {
        public String name;
        public int age;

        // default constructor required for Jackson
        public TestObject() {
        }

        public TestObject(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof TestObject other)) return false;
            return this.name.equals(other.name) && this.age == other.age;
        }
    }

    @Test
    public void testSerializeDeserializeObject() {
        TestObject original = new TestObject("John", 30);
        String json = JsonUtils.toJson(original);
        TestObject result = JsonUtils.deserializeJsonToObject(json, TestObject.class);
        Assertions.assertEquals(original, result);
    }

    @Test
    public void testDeserializeJsonToList() {
        String json = "[{\"name\":\"Alice\",\"age\":25},{\"name\":\"Bob\",\"age\":40}]";
        List<TestObject> list = JsonUtils.deserializeJsonToList(json, TestObject.class);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals("Alice", list.get(0).name);
        Assertions.assertEquals(40, list.get(1).age);
    }

    @Test
    public void testToJson() {
        TestObject obj = new TestObject("Kate", 22);
        String json = JsonUtils.toJson(obj);
        Assertions.assertTrue(json.contains("\"name\":\"Kate\""));
        Assertions.assertTrue(json.contains("\"age\":22"));
    }

    @Test
    public void testFromJsonList() {
        String json = "[{\"key1\":\"value1\"},{\"key2\":\"value2\"}]";
        List<Map<String, String>> list = JsonUtils.fromJsonList(json);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals("value1", list.get(0).get("key1"));
        Assertions.assertEquals("value2", list.get(1).get("key2"));
    }

    @Test
    public void testEscapeJson() {
        String input = "Hello \"World\"\n";
        String escaped = JsonUtils.escapeJson(input);
        Assertions.assertEquals("Hello \\\"World\\\"\\n", escaped);
    }
}