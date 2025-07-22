package com.avpuser.file;

import java.util.Optional;

public class FileTypeUtils {

    public static Optional<FileType> detectFileType(byte[] fileData) {
        if (fileData == null || fileData.length < 12) {
            return Optional.empty();
        }

        if (startsWith(fileData, "%PDF".getBytes())) {
            return Optional.of(FileType.PDF);
        }


        if (startsWith(fileData, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47})) {
            return Optional.of(FileType.PNG);
        }

        if (startsWith(fileData, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
            return Optional.of(FileType.JPEG);
        }

        if (startsWith(fileData, "GIF87a".getBytes()) || startsWith(fileData, "GIF89a".getBytes())) {
            return Optional.of(FileType.GIF);
        }

        if (startsWith(fileData, new byte[]{0x42, 0x4D})) {
            return Optional.of(FileType.BMP);
        }

        if (startsWith(fileData, new byte[]{0x49, 0x49, 0x2A, 0x00}) || startsWith(fileData, new byte[]{0x4D, 0x4D, 0x00, 0x2A})) {
            return Optional.of(FileType.TIFF);
        }

        if (startsWith(fileData, "RIFF".getBytes()) &&
                fileData.length >= 12 &&
                new String(fileData, 8, 4).equals("WEBP")) {
            return Optional.of(FileType.WEBP);
        }

        return Optional.empty();
    }

    public static boolean isPdf(byte[] fileBytes) {
        return FileTypeUtils.detectFileType(fileBytes)
                .map(t -> t == FileType.PDF)
                .orElse(false);
    }

    private static boolean startsWith(byte[] source, byte[] prefix) {
        if (source.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (source[i] != prefix[i]) return false;
        }
        return true;
    }
}