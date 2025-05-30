package utils;

import com.avpuser.utils.NumberFormatter;
import org.junit.Assert;
import org.junit.Test;

public class NumberFormatterTest {

    @Test
    public void testFormatNumber() {
        // Numbers less than 1000
        Assert.assertEquals("950", NumberFormatter.formatNumber(950));
        Assert.assertEquals("1", NumberFormatter.formatNumber(1));
        Assert.assertEquals("0", NumberFormatter.formatNumber(0));

        // Numbers from 1000 to less than 1 million
        Assert.assertEquals("1.2K", NumberFormatter.formatNumber(1_200));
        Assert.assertEquals("52.8K", NumberFormatter.formatNumber(52_800));
        Assert.assertEquals("999.9K", NumberFormatter.formatNumber(999_900));

        // Numbers from 1 million to less than 1 billion
        Assert.assertEquals("1.0M", NumberFormatter.formatNumber(1000_000));
        Assert.assertEquals("1.2M", NumberFormatter.formatNumber(1_230_000));
        Assert.assertEquals("1.3M", NumberFormatter.formatNumber(1_250_000));
        Assert.assertEquals("745.0M", NumberFormatter.formatNumber(745_000_000));

        // Numbers from 1 billion and above
        Assert.assertEquals("1.2B", NumberFormatter.formatNumber(1_234_567_890));
        Assert.assertEquals("2.0B", NumberFormatter.formatNumber(2_000_000_000L));
        Assert.assertEquals("1.0T", NumberFormatter.formatNumber(1_000_000_000_000L));
    }
}
