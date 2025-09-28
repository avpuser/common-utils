package com.avpuser.s3;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Locale;

public class S3StorageClient {

    private static final Logger logger = LogManager.getLogger(S3StorageClient.class);

    private final S3Client s3Client;
    private final S3TransferManager transferManager;
    private final S3AsyncClient s3AsyncClient;

    private final String bucketName;
    private final String region;
    private final String domain;
    private final String accessKey;
    private final String secretKey;

    public S3StorageClient(String accessKey, String secretKey, String region, String bucketName, String domain) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create("https://" + domain))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        this.s3AsyncClient = S3AsyncClient.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create("https://" + domain))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                // настройки ниже оставлены как в исходном коде
                .multipartEnabled(true)
                .multipartConfiguration(cfg -> cfg
                        .thresholdInBytes(8L * 1024 * 1024)
                        .minimumPartSizeInBytes(64L * 1024 * 1024)
                        .apiCallBufferSizeInBytes(32L * 1024 * 1024)
                )
                .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                        .maxConcurrency(64)
                        .readTimeout(Duration.ofMinutes(10))
                        .writeTimeout(Duration.ofMinutes(10))
                        .connectionTimeout(Duration.ofSeconds(30))
                        .tcpKeepAlive(true)
                )
                .build();

        this.transferManager = S3TransferManager.builder()
                .s3Client(s3AsyncClient)
                .build();

        this.bucketName = bucketName;
        this.region = region;
        this.domain = domain;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    // -------------------------
    // Helpers
    // -------------------------

    public static String extractFileNameFromS3Key(String s3Key) {
        if (s3Key == null || s3Key.trim().isEmpty()) {
            return "unknown";
        }
        int lastSlash = s3Key.lastIndexOf('/');
        String fileName = lastSlash >= 0 ? s3Key.substring(lastSlash + 1) : s3Key;
        return fileName.isBlank() ? "unknown" : fileName;
    }

    private static boolean isGzipKey(String key) {
        return key != null && key.toLowerCase(Locale.ROOT).endsWith(".gz");
    }

    private static String guessContentType(String key, boolean gz) {
        if (gz) return "application/gzip";
        if (key != null && (key.endsWith(".log") || key.endsWith(".txt"))) {
            return "text/plain; charset=UTF-8";
        }
        return "application/octet-stream";
    }

    private static String guessContentDisposition(String key) {
        String name = extractFileNameFromS3Key(key);
        return "inline; filename=\"" + name + "\"";
    }

    public File toTempFile(byte[] data, String fileName) {
        try {
            File tempFile = File.createTempFile("upload-", "-" + fileName);
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(data);
            }
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить файл во временное хранилище", e);
        }
    }

    // -------------------------
    // Upload (bytes) - default (auto headers)
    // -------------------------

    public String uploadFile(byte[] fileData, String remoteFilePath) {
        boolean gz = isGzipKey(remoteFilePath);
        String ct = guessContentType(remoteFilePath, gz);
        String ce = gz ? "gzip" : null;
        String cd = guessContentDisposition(remoteFilePath);
        return uploadFile(fileData, remoteFilePath, ct, ce, cd);
    }

    // -------------------------
    // Upload (bytes) - explicit headers
    // -------------------------

    public String uploadFile(byte[] fileData, String remoteFilePath,
                             String contentType, String contentEncoding, String contentDisposition) {
        String fileName = extractFileNameFromS3Key(remoteFilePath);
        File temp = toTempFile(fileData, fileName);
        try {
            return uploadFile(temp.getAbsolutePath(), remoteFilePath, contentType, contentEncoding, contentDisposition);
        } finally {
            if (!temp.delete()) {
                logger.error("Unable to delete temporary file: {}", temp.getAbsolutePath());
            }
        }
    }

    // -------------------------
    // Upload (file path) - default (auto headers)
    // -------------------------

    public String uploadFile(String localFilePath, String remoteFilePath) {
        boolean gz = isGzipKey(remoteFilePath);
        String ct = guessContentType(remoteFilePath, gz);
        String ce = gz ? "gzip" : null;
        String cd = guessContentDisposition(remoteFilePath);
        return uploadFile(localFilePath, remoteFilePath, ct, ce, cd);
    }

    // -------------------------
    // Upload (file path) - explicit headers
    // -------------------------

    public String uploadFile(String localFilePath, String remoteFilePath,
                             String contentType, String contentEncoding, String contentDisposition) {
        File file = new File(localFilePath);
        if (!file.exists()) {
            logger.error("File not found: " + localFilePath);
            throw new IllegalArgumentException("File not found: " + localFilePath);
        }

        try {
            PutObjectRequest.Builder b = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(remoteFilePath);

            if (contentType != null)        b = b.contentType(contentType);
            if (contentEncoding != null)    b = b.contentEncoding(contentEncoding);
            if (contentDisposition != null) b = b.contentDisposition(contentDisposition);

            PutObjectRequest putObjectRequest = b.build();

            logger.info("Uploading file to S3: {} (ct={}, enc={})",
                    remoteFilePath, contentType, contentEncoding);

            PutObjectResponse putObjectResponse = s3Client.putObject(putObjectRequest, Paths.get(localFilePath));
            logger.info("File successfully uploaded to S3: {}", remoteFilePath);
            return buildS3Url(remoteFilePath);
        } catch (S3Exception e) {
            logger.error("Error uploading file: " + e.awsErrorDetails().errorMessage(), e);
            throw e;
        }
    }

    // -------------------------
    // Multipart upload (auto headers)
    // -------------------------

    public String uploadFileMultipart(String localFilePath, String remoteFilePath) {
        File file = new File(localFilePath);
        if (!file.exists()) {
            logger.error("File not found: {}", localFilePath);
            throw new IllegalArgumentException("File not found: " + localFilePath);
        }

        boolean gz = isGzipKey(remoteFilePath);
        String ct = guessContentType(remoteFilePath, gz);
        String ce = gz ? "gzip" : null;
        String cd = guessContentDisposition(remoteFilePath);

        logger.info("Multipart upload to S3: {} ({} bytes, ct={}, enc={})",
                remoteFilePath, file.length(), ct, ce);

        UploadFileRequest request = UploadFileRequest.builder()
                .putObjectRequest(b -> {
                    b.bucket(bucketName)
                            .key(remoteFilePath)
                            .contentType(ct)
                            .contentDisposition(cd);
                    if (ce != null) b.contentEncoding(ce);
                })
                .source(Paths.get(localFilePath))
                .addTransferListener(LoggingTransferListener.create())
                .build();

        FileUpload upload = transferManager.uploadFile(request);
        upload.completionFuture().join();

        logger.info("Multipart upload completed: {}", remoteFilePath);
        return buildS3Url(remoteFilePath);
    }

    // -------------------------
    // Misc
    // -------------------------

    public String buildS3Url(String remoteFilePath) {
        return "https://" + bucketName + "." + domain + "/" + remoteFilePath;
    }

    public boolean fileExists(String remoteRelativePath) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(remoteRelativePath)
                    .build();

            s3Client.headObject(headObjectRequest);
            logger.info("File exists on S3: " + remoteRelativePath);
            return true;
        } catch (S3Exception e) {
            if ("NoSuchKey".equals(e.awsErrorDetails().errorCode())) {
                logger.info("File does not exist on S3: " + remoteRelativePath);
                return false;
            }
            logger.error("Error checking file existence on S3: " + e.awsErrorDetails().errorMessage(), e);
            throw e;
        }
    }

    public void deleteFile(String remoteRelativePath) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(remoteRelativePath)
                    .build();

            logger.info("Deleting file from S3: " + remoteRelativePath);

            DeleteObjectResponse deleteObjectResponse = s3Client.deleteObject(deleteObjectRequest);

            logger.info("File successfully deleted from S3: " + remoteRelativePath);
        } catch (S3Exception e) {
            logger.error("Error deleting file: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    // -------------------------
    // Presign GET
    // -------------------------

    public String generatePresignedUrl(String remoteRelativePath, Duration duration) {
        return generatePresignedUrl(remoteRelativePath, duration, null);
    }

    /**
     * Генерирует presigned GET URL. Если передать responseContentType, он будет
     * проставлен в ответе (например, "text/plain; charset=UTF-8").
     */
    public String generatePresignedUrl(String remoteRelativePath, Duration duration, String responseContentType) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create("https://" + domain))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build()) {

            GetObjectRequest.Builder get = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(remoteRelativePath);

            if (responseContentType != null) {
                get = get.responseContentType(responseContentType);
            }

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .getObjectRequest(get.build())
                    .build();

            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);

            logger.info("Generated pre-signed URL: " + presignedRequest.url());

            return presignedRequest.url().toString();
        }
    }
}