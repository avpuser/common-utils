package s3;

import com.avpuser.test.MockTest;
import com.avpuser.s3.S3ObjectListResult;
import com.avpuser.s3.S3ObjectMetadata;
import com.avpuser.s3.S3StorageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@MockTest

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
        S3Exception error = (S3Exception) S3Exception.builder()
                .message("Fail")
                .statusCode(500)
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("InternalError")
                        .errorMessage("Fail")
                        .build())
                .build();

        when(mockS3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(error);

        S3Exception thrown = assertThrows(
                S3Exception.class,
                () -> s3StorageClient.deleteFile("delete/fail.txt")
        );

        assertSame(error, thrown);

        verify(mockS3Client, times(1))
                .deleteObject(any(DeleteObjectRequest.class));
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

    @Test
    void listObjects_mapsSummariesAndContinuationToken() {
        Instant modified = Instant.parse("2026-01-02T03:04:05Z");
        S3Object obj = S3Object.builder()
                .key("assets/a.png")
                .size(12L)
                .lastModified(modified)
                .eTag("\"etag1\"")
                .build();
        when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder()
                        .contents(obj)
                        .isTruncated(true)
                        .nextContinuationToken("token-2")
                        .build());

        S3ObjectListResult result = s3StorageClient.listObjects(50, null);

        assertEquals(1, result.objects().size());
        assertEquals("assets/a.png", result.objects().get(0).key());
        assertEquals(12L, result.objects().get(0).size());
        assertEquals(modified, result.objects().get(0).lastModified());
        assertEquals("\"etag1\"", result.objects().get(0).eTag());
        assertTrue(result.truncated());
        assertEquals("token-2", result.nextContinuationToken());

        ArgumentCaptor<ListObjectsV2Request> captor = ArgumentCaptor.forClass(ListObjectsV2Request.class);
        verify(mockS3Client).listObjectsV2(captor.capture());
        assertEquals("my-bucket", captor.getValue().bucket());
        assertEquals(50, captor.getValue().maxKeys());
    }

    @Test
    void listObjects_passesContinuationToken() {
        when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().contents(List.of()).isTruncated(false).build());

        s3StorageClient.listObjects(10, "cont-1");

        ArgumentCaptor<ListObjectsV2Request> captor = ArgumentCaptor.forClass(ListObjectsV2Request.class);
        verify(mockS3Client).listObjectsV2(captor.capture());
        assertEquals("cont-1", captor.getValue().continuationToken());
        assertEquals(10, captor.getValue().maxKeys());
    }

    @Test
    void headObject_returnsMetadata() {
        Instant modified = Instant.parse("2026-03-04T05:06:07Z");
        when(mockS3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder()
                        .contentType("image/png")
                        .contentLength(99L)
                        .lastModified(modified)
                        .eTag("\"abc\"")
                        .metadata(Map.of("original", "logo.png"))
                        .build());

        S3ObjectMetadata metadata = s3StorageClient.headObject("assets/x.png");

        assertEquals("assets/x.png", metadata.key());
        assertEquals("image/png", metadata.contentType());
        assertEquals(99L, metadata.contentLength());
        assertEquals(modified, metadata.lastModified());
        assertEquals("\"abc\"", metadata.eTag());
        assertEquals("logo.png", metadata.metadata().get("original"));
    }

    @Test
    void headObject_notFound_rethrows() {
        S3Exception notFound = (S3Exception) S3Exception.builder()
                .statusCode(404)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("NotFound").build())
                .message("Not found")
                .build();
        when(mockS3Client.headObject(any(HeadObjectRequest.class))).thenThrow(notFound);

        S3Exception thrown = assertThrows(S3Exception.class, () -> s3StorageClient.headObject("missing"));
        assertSame(notFound, thrown);
    }
}