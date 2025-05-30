package utils;

import com.avpuser.utils.UrlUtils;
import org.junit.Assert;
import org.junit.Test;

public class UrlUtilsTest {

    @Test
    public void getUrlImageFileExtensionTest() {
        Assert.assertEquals(".jpg", UrlUtils.getUrlImageFileExtension("https://steamuserimages-a.akamaihd.net/ugc/52453354080448818/543783B601D5A853E3F50907B9722A314DFD92B6/?imw=512&amp;imh=320&amp;ima=fit&amp;impolicy=Letterbox&amp;imcolor=%23000000&amp;letterbox=true"));
        Assert.assertEquals(".png", UrlUtils.getUrlImageFileExtension("https://i.ytimg.com/vi/1234/maxresdefault.png"));
        Assert.assertEquals(".jpg", UrlUtils.getUrlImageFileExtension("https://i.ytimg.com/vi/1234/sddefault.jpg"));
    }

}
