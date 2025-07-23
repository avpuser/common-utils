package s3;

import com.avpuser.s3.S3StorageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class S3StorageClientTest {

    private S3Client mockS3Client;
    private S3StorageClient s3StorageClient;

    @BeforeEach
    void setUp() {
        // Подменяем создание клиента через spy, если getS3Client выделен в отдельный метод
        mockS3Client = mock(S3Client.class);

        s3StorageClient = Mockito.spy(new S3StorageClient(
                "accessKey",
                "secretKey",
                "us-east-1",
                "my-bucket",
                "s3.amazonaws.com"
        ));

        // Заменим внутренний s3Client через reflection (если он final — может потребоваться изменить код)
        try {
            var field = S3StorageClient.class.getDeclaredField("s3Client");
            field.setAccessible(true);
            field.set(s3StorageClient, mockS3Client);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testBuildS3Url() {
        String url = s3StorageClient.buildS3Url("folder/file.txt");
        assertEquals("https://my-bucket.s3.amazonaws.com/folder/file.txt", url);
    }

    @Test
    void testFileExists_true() {
        when(mockS3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        boolean exists = s3StorageClient.fileExists("some/file.txt");
        assertTrue(exists);
    }

    @Test
    void testFileExists_false_NoSuchKey() {
        S3Exception noSuchKeyException = (S3Exception) S3Exception.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchKey").build())
                .statusCode(404)
                .message("Not found")
                .build();

        when(mockS3Client.headObject(any(HeadObjectRequest.class))).thenThrow(noSuchKeyException);

        boolean exists = s3StorageClient.fileExists("missing/file.txt");
        assertFalse(exists);
    }

    @Test
    void testFileExists_throwsOtherException() {
        AwsServiceException internalError = S3Exception.builder()
                .message("Something went wrong")
                .statusCode(500)
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("InternalError")
                        .errorMessage("Something went wrong")
                        .build())
                .build();

        when(mockS3Client.headObject(any(HeadObjectRequest.class))).thenThrow(internalError);

        AwsServiceException thrown = assertThrows(AwsServiceException.class, () -> {
            s3StorageClient.fileExists("file.txt");
        });

        assertEquals("Something went wrong", thrown.awsErrorDetails().errorMessage());
    }

    @Test
    void testDeleteFile_success() {
        DeleteObjectResponse response = DeleteObjectResponse.builder().build();
        when(mockS3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(response);

        assertDoesNotThrow(() -> s3StorageClient.deleteFile("delete/this.txt"));
        verify(mockS3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void testDeleteFile_withException() {
        S3Exception error = mock(S3Exception.class);
        AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorCode("InternalError")
                .errorMessage("Fail")
                .build();

        when(error.awsErrorDetails()).thenReturn(errorDetails);

        when(mockS3Client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(error);

        // Метод deleteFile проглатывает исключение, поэтому не должно выбрасываться
        assertDoesNotThrow(() -> s3StorageClient.deleteFile("delete/fail.txt"));
        verify(mockS3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void testUploadFile_success() throws IOException {
        // Создаем временный файл
        File tempFile = File.createTempFile("test-upload-", ".txt");
        tempFile.deleteOnExit();

        String localPath = tempFile.getAbsolutePath();
        String remotePath = "folder/testfile.txt";

        // Заполняем файл каким-то содержимым
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write("hello world".getBytes());
        }

        when(mockS3Client.putObject(any(PutObjectRequest.class), any(Path.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String result = s3StorageClient.uploadFile(localPath, remotePath);
        assertEquals("https://my-bucket.s3.amazonaws.com/folder/testfile.txt", result);
    }

    @Test
    void testUploadFile_missingLocalFile_throwsException() {
        String missingPath = "/path/to/missing/file.txt";

        assertThrows(IllegalArgumentException.class, () ->
                s3StorageClient.uploadFile(missingPath, "remote/file.txt"));
    }

    @Test
    void testExtractFileNameFromS3Key_variousCases() {
        assertEquals("file.txt", S3StorageClient.extractFileNameFromS3Key("folder/file.txt"));
        assertEquals("file.txt", S3StorageClient.extractFileNameFromS3Key("file.txt"));
        assertEquals("unknown", S3StorageClient.extractFileNameFromS3Key(""));
        assertEquals("unknown", S3StorageClient.extractFileNameFromS3Key(null));
        assertEquals("unknown", S3StorageClient.extractFileNameFromS3Key("folder/"));
    }
}