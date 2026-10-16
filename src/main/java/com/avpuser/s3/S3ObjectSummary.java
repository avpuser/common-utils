package com.avpuser.s3;

import java.time.Instant;

public record S3ObjectSummary(
        String key,
        Long size,
        Instant lastModified,
        String eTag
) {
}
