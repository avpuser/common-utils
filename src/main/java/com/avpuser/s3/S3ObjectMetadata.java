package com.avpuser.s3;

import java.time.Instant;
import java.util.Map;

public record S3ObjectMetadata(
        String key,
        String contentType,
        Long contentLength,
        Instant lastModified,
        String eTag,
        Map<String, String> metadata
) {
}
