package com.avpuser.file;

public enum FileType {
    PDF(".pdf", "application/pdf"),
    PNG(".png", "image/png"),
    JPEG(".jpg", "image/jpeg"),
    GIF(".gif", "image/gif"),
    BMP(".bmp", "image/bmp"),
    TIFF(".tif", "image/tiff"),
    WEBP(".webp", "image/webp"),
    DOCX(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"), // добавлено
    ;

    private final String extension;
    private final String mimeType;

    FileType(String extension, String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }

    public String getExtension() {
        return extension;
    }

    public String getMimeType() {
        return mimeType;
    }
}