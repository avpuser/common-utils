package com.avpuser.file;

import lombok.Getter;

public enum FileType {
    PDF(".pdf", "application/pdf", false, true),
    PNG(".png", "image/png", true, false),
    JPEG(".jpg", "image/jpeg", true, false),
    GIF(".gif", "image/gif", true, false),
    BMP(".bmp", "image/bmp", true, false),
    TIFF(".tif", "image/tiff", true, false),
    WEBP(".webp", "image/webp", true, false),
    DOCX(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", false, false),
    TXT(".txt", "text/plain", false, false),
    ;

    @Getter
    private final String extension;
    @Getter
    private final String mimeType;
    @Getter
    private final boolean isImage;
    @Getter
    private final boolean isPdf;

    FileType(String extension, String mimeType, boolean isImage, boolean isPdf) {
        this.extension = extension;
        this.mimeType = mimeType;
        this.isImage = isImage;
        this.isPdf = isPdf;
    }

}