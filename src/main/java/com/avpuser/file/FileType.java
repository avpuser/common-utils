package com.avpuser.file;

public enum FileType {
    PDF(".pdf"),
    PNG(".png"),
    JPEG(".jpg"),
    GIF(".gif"),
    BMP(".bmp"),
    TIFF(".tif"),
    WEBP(".webp");

    private final String extension;

    FileType(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}
