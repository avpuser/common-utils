package com.avpuser.utils;

import com.avpuser.file.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;

public class UrlUtils {

    public static byte[] download(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        connection.setRequestMethod("GET");

        int statusCode = connection.getResponseCode();
        if (statusCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP error code: " + statusCode);
        }

        try (InputStream in = connection.getInputStream()) {
            return in.readAllBytes();
        }
    }

    public static void downloadImage(String imageUrl, String destinationFile) {
        FileUtils.createDirectoriesIfNotExist(destinationFile);
        URL url;
        try {
            url = new URL(imageUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        try (InputStream in = url.openStream()) {
            Files.copy(in, Paths.get(destinationFile), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getUrlImageFileExtension(String fileUrl) {
        Optional<String> extensionFromUrl = getExtensionFromUrlIfPresent(fileUrl);
        if (extensionFromUrl.isPresent()) {
            return extensionFromUrl.get();
        }

        String fileExtension;
        try {
            URL url = new URL(fileUrl);
            URLConnection connection = url.openConnection();
            // Устанавливаем таймауты
            connection.setConnectTimeout(500); // 500 мс на подключение
            connection.setReadTimeout(500);    // 500 мс на чтение данных

            String contentType = connection.getContentType();

            if (contentType != null) {
                fileExtension = switch (contentType) {
                    case "image/jpeg" -> ".jpg";
                    case "image/png" -> ".png";
                    case "image/gif" -> ".gif";
                    case "image/bmp" -> ".bmp";
                    case "image/webp" -> ".webp";
                    case "image/svg+xml" -> ".svg";
                    case "image/tiff" -> ".tiff";
                    case "image/vnd.adobe.photoshop" -> ".psd";
                    case "image/x-icon" -> ".ico";
                    case "image/heic" -> ".heic";
                    case "image/heif" -> ".heif";
                    case "image/x-ms-bmp" -> ".bmp";
                    case "image/x-cmu-raster" -> ".ras";
                    case "image/x-portable-anymap" -> ".pnm";
                    case "image/x-portable-bitmap" -> ".pbm";
                    case "image/x-portable-graymap" -> ".pgm";
                    case "image/x-portable-pixmap" -> ".ppm";
                    case "image/x-rgb" -> ".rgb";
                    default -> throw new RuntimeException("Unknown content type: " + contentType);
                };
            } else {
                throw new RuntimeException("Invalid content type: " + fileUrl);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error during get content type: " + fileUrl, e);
        }

        return fileExtension;
    }

    private static final Map<String, String> EXTENSION_MAP = Map.ofEntries(
            Map.entry(".jpg", ".jpg"),
            Map.entry(".jpeg", ".jpg"),
            Map.entry(".png", ".png"),
            Map.entry(".gif", ".gif"),
            Map.entry(".bmp", ".bmp"),
            Map.entry(".webp", ".webp"),
            Map.entry(".svg", ".svg"),
            Map.entry(".tiff", ".tiff"),
            Map.entry(".psd", ".psd"),
            Map.entry(".ico", ".ico"),
            Map.entry(".heic", ".heic"),
            Map.entry(".heif", ".heif"),
            Map.entry(".ras", ".ras"),
            Map.entry(".pnm", ".pnm"),
            Map.entry(".pbm", ".pbm"),
            Map.entry(".pgm", ".pgm"),
            Map.entry(".ppm", ".ppm"),
            Map.entry(".rgb", ".rgb")
    );

    private static Optional<String> getExtensionFromUrlIfPresent(String fileUrl) {
        String lowerCaseUrl = fileUrl.toLowerCase();
        return EXTENSION_MAP.entrySet().stream()
                .filter(entry -> lowerCaseUrl.endsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }
}
