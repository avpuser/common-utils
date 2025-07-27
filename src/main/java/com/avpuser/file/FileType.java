package com.avpuser.file;

import lombok.Getter;

public enum FileType {
    PDF(".pdf", "application/pdf", false),
    PNG(".png", "image/png", true),
    JPEG(".jpg", "image/jpeg", true),
    GIF(".gif", "image/gif", true),
    BMP(".bmp", "image/bmp", true),
    TIFF(".tif", "image/tiff", true),
    WEBP(".webp", "image/webp", true),
    DOCX(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", false),
    TXT(".txt", "text/plain", false),
    ;

    @Getter
    private final String extension;
    @Getter
    private final String mimeType;
    @Getter
    private final boolean isImage;

    FileType(String extension, String mimeType, boolean isImage) {
        this.extension = extension;
        this.mimeType = mimeType;
        this.isImage = isImage;
    }

}