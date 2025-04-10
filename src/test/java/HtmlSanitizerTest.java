import com.avpuser.utils.HtmlSanitizer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HtmlSanitizerTest {

    @Test
    public void testNullInput() {
        String input = null;
        String expected = null;
        assertEquals(expected, HtmlSanitizer.escapeHtml(input));
    }

    @Test
    public void testEmptyInput() {
        String input = "";
        String expected = "";
        assertEquals(expected, HtmlSanitizer.escapeHtml(input));
    }

    @Test
    public void testPreserveTags() {
        String input = "Text with <b>bold</b> and <i>italic</i> tags.";
        String expected = "Text with <b>bold</b> and <i>italic</i> tags.";
        assertEquals(expected, HtmlSanitizer.escapeHtml(input));
    }

    @Test
    public void testEscapeSpecialChars() {
        String input = "Text with < and > and & symbols.";
        String expected = "Text with &lt; and &gt; and &amp; symbols.";
        assertEquals(expected, HtmlSanitizer.escapeHtml(input));
    }

    @Test
    public void testMixedContent() {
        String input = "Some <b>bold & <</b> text > here & more.";
        String expected = "Some <b>bold &amp; &lt;</b> text &gt; here &amp; more.";
        assertEquals(expected, HtmlSanitizer.escapeHtml(input));
    }

    @Test
    public void testUnclosedTags() {
        String input = "Text with <b>bold text and < not a tag";
        String expected = "Text with <b>bold text and &lt; not a tag</b>";
        assertEquals(expected, HtmlSanitizer.escapeHtml(input));
    }

    @Test
    public void testOnlySpecialChars() {
        String input = "<>&";
        String expected = "&lt;&gt;&amp;";
        assertEquals(expected, HtmlSanitizer.escapeHtml(input));
    }

    @Test
    public void testHtmlEntitiesUnchanged() {
        String input = "Already encoded: &amp; &lt; &gt;";
        String expected = "Already encoded: &amp; &lt; &gt;";
        assertEquals(expected, HtmlSanitizer.escapeHtml(input));
    }

}
