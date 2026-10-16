package com.avpuser.s3;

import java.util.List;

public record S3ObjectListResult(
        List<S3ObjectSummary> objects,
        boolean truncated,
        String nextContinuationToken
) {
}
