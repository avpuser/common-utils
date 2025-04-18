import com.avpuser.utils.JsonUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

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
            if (!(obj instanceof TestObject)) return false;
            TestObject other = (TestObject) obj;
            return this.name.equals(other.name) && this.age == other.age;
        }
    }

    @Test
    public void testSerializeDeserializeObject() {
        TestObject original = new TestObject("John", 30);
        String json = JsonUtils.toJson(original);
        TestObject result = JsonUtils.deserializeJsonToObject(json, TestObject.class);
        Assert.assertEquals(original, result);
    }

    @Test
    public void testDeserializeJsonToList() {
        String json = "[{\"name\":\"Alice\",\"age\":25},{\"name\":\"Bob\",\"age\":40}]";
        List<TestObject> list = JsonUtils.deserializeJsonToList(json, TestObject.class);
        Assert.assertEquals(2, list.size());
        Assert.assertEquals("Alice", list.get(0).name);
        Assert.assertEquals(40, list.get(1).age);
    }

    @Test
    public void testToJson() {
        TestObject obj = new TestObject("Kate", 22);
        String json = JsonUtils.toJson(obj);
        Assert.assertTrue(json.contains("\"name\":\"Kate\""));
        Assert.assertTrue(json.contains("\"age\":22"));
    }

    @Test
    public void testFromJsonList() {
        String json = "[{\"key1\":\"value1\"},{\"key2\":\"value2\"}]";
        List<Map<String, String>> list = JsonUtils.fromJsonList(json);
        Assert.assertEquals(2, list.size());
        Assert.assertEquals("value1", list.get(0).get("key1"));
        Assert.assertEquals("value2", list.get(1).get("key2"));
    }

    @Test
    public void testEscapeJson() {
        String input = "Hello \"World\"\n";
        String escaped = JsonUtils.escapeJson(input);
        Assert.assertEquals("Hello \\\"World\\\"\\n", escaped);
    }
}