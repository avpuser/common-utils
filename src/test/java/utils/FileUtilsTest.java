package utils;

import com.avpuser.file.FileUtils;
import org.junit.Assert;
import org.junit.Test;

public class FileUtilsTest {

    @Test
    public void getFileNameWithoutExtensionTest() {
        Assert.assertEquals("hEllo",
                FileUtils.getFileNameWithoutExtension("/Folder/Folder2/Folder3/hEllo.Mp3"));

        Assert.assertEquals("hEllo",
                FileUtils.getFileNameWithoutExtension("hEllo.Mmmp3"));
    }

}
