package s3;

import com.avpuser.s3.S3StorageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

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
}