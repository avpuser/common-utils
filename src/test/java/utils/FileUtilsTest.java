package utils;

import com.avpuser.test.MockTest;
import com.avpuser.file.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
@MockTest

public class FileUtilsTest {

    @Test
    public void getFileNameWithoutExtensionTest() {
        Assertions.assertEquals("hEllo",
                FileUtils.getFileNameWithoutExtension("/Folder/Folder2/Folder3/hEllo.Mp3"));

        Assertions.assertEquals("hEllo",
                FileUtils.getFileNameWithoutExtension("hEllo.Mmmp3"));
    }

}
