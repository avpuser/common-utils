package com.avpuser.s3;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.time.Duration;

public class S3StorageClient {

    private static final Logger logger = LogManager.getLogger(S3StorageClient.class);

    private final S3Client s3Client;

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
        this.bucketName = bucketName;
        this.region = region;
        this.domain = domain;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    private static String extractFileNameFromS3Key(String s3Key) {
        if (s3Key == null || s3Key.trim().isEmpty()) {
            return "unknown";
        }
        int lastSlash = s3Key.lastIndexOf('/');
        String fileName = lastSlash >= 0 ? s3Key.substring(lastSlash + 1) : s3Key;
        return fileName.isBlank() ? "unknown" : fileName;
    }

    public String uploadFile(byte[] fileData, String remoteFilePath) {
        String fileName = extractFileNameFromS3Key(remoteFilePath);
        File temp = toTempFile(fileData, fileName);
        try {
            return uploadFile(temp.getAbsolutePath(), remoteFilePath);
        } finally {
            if (!temp.delete()) {
                logger.error("Unable to delete temporary file: {}", temp.getAbsolutePath());
            }
        }
    }

    private File toTempFile(byte[] data, String fileName) {
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


    public String uploadFile(String localFilePath, String remoteFilePath) {
        File file = new File(localFilePath);

        // Log if file does not exist
        if (!file.exists()) {
            logger.error("File not found: " + localFilePath);
            throw new IllegalArgumentException("File not found: " + localFilePath);
        }


        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(remoteFilePath)
                    .build();

            // Log the start of the upload
            logger.info("Uploading file to S3: " + remoteFilePath);

            PutObjectResponse putObjectResponse = s3Client.putObject(putObjectRequest, Paths.get(localFilePath));

            // Log success
            logger.info("File successfully uploaded to S3: " + remoteFilePath);
            return buildS3Url(remoteFilePath);
        } catch (S3Exception e) {
            // Log the error message
            logger.error("Error uploading file: " + e.awsErrorDetails().errorMessage(), e);
            throw e;
        }
    }

    public String buildS3Url(String remoteFilePath) {
        String resUrl = "https://" + bucketName + "." + domain + "/" + remoteFilePath;
        return resUrl;
    }

    public boolean fileExists(String remoteRelativePath) {
        try {
            // Try to get metadata of the object (head request)
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(remoteRelativePath)
                    .build();

            s3Client.headObject(headObjectRequest); // If file exists, this will return without error

            // If we reach here, file exists
            logger.info("File exists on S3: " + remoteRelativePath);
            return true;
        } catch (S3Exception e) {
            if (e.awsErrorDetails().errorCode().equals("NoSuchKey")) {
                // If the error is NoSuchKey, the file does not exist
                logger.info("File does not exist on S3: " + remoteRelativePath);
                return false;
            }
            // Log any other exception
            logger.error("Error checking file existence on S3: " + e.awsErrorDetails().errorMessage(), e);
            throw e;
        }
    }

    public void deleteFile(String remoteRelativePath) {
        try {
            // Create the delete request
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(remoteRelativePath)
                    .build();

            // Log the start of the deletion process
            logger.info("Deleting file from S3: " + remoteRelativePath);

            // Execute the delete request
            DeleteObjectResponse deleteObjectResponse = s3Client.deleteObject(deleteObjectRequest);

            // Log successful deletion
            logger.info("File successfully deleted from S3: " + remoteRelativePath);
        } catch (S3Exception e) {
            // Log the error message
            logger.error("Error deleting file: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    public String generatePresignedUrl(String remoteRelativePath, Duration duration) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create("https://" + domain))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build()) {

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(remoteRelativePath)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);

            logger.info("Generated pre-signed URL: " + presignedRequest.url());

            return presignedRequest.url().toString();
        }
    }
}
