package com.avpuser.utils;

import com.avpuser.file.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtils {

    /** Matches {@code IOException("HTTP error code: N")} thrown by {@link #download}. */
    private static final Pattern HTTP_ERROR_CODE_PATTERN =
            Pattern.compile("HTTP error code:\\s*(\\d{3})", Pattern.CASE_INSENSITIVE);

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

    /** Default download: connect 5s, read 10s. Prefer the overload for longer transfers. */
    public static byte[] download(String urlString) {
        return download(urlString, 5_000, 10_000);
    }

    public static byte[] download(String urlString, int connectTimeoutMs, int readTimeoutMs) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setRequestMethod("GET");

            int statusCode = connection.getResponseCode();
            if (statusCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + statusCode);
            }

            try (InputStream in = connection.getInputStream()) {
                return in.readAllBytes();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Whether a failure from {@link #download} is worth retrying (transient network / overload).
     * Permanent client errors (typical HTTP 4xx except 408/429) are not retryable.
     */
    public static boolean isRetryableDownloadFailure(Throwable throwable) {
        OptionalInt httpStatus = extractHttpErrorCode(throwable);
        if (httpStatus.isPresent()) {
            int code = httpStatus.getAsInt();
            return code == 408 || code == 429 || (code >= 500 && code <= 599);
        }
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current instanceof SocketTimeoutException
                    || current instanceof ConnectException
                    || current instanceof NoRouteToHostException
                    || current instanceof UnknownHostException) {
                return true;
            }
            // Connection reset / broken pipe during transfer — usually transient.
            if (current instanceof SocketException) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parses {@code HTTP error code: NNN} from the throwable cause chain, if present.
     */
    public static OptionalInt extractHttpErrorCode(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message == null || message.isBlank()) {
                continue;
            }
            Matcher matcher = HTTP_ERROR_CODE_PATTERN.matcher(message);
            if (matcher.find()) {
                try {
                    return OptionalInt.of(Integer.parseInt(matcher.group(1)));
                } catch (NumberFormatException ignored) {
                    // continue scanning
                }
            }
        }
        return OptionalInt.empty();
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

    private static Optional<String> getExtensionFromUrlIfPresent(String fileUrl) {
        String lowerCaseUrl = fileUrl.toLowerCase();
        return EXTENSION_MAP.entrySet().stream()
                .filter(entry -> lowerCaseUrl.endsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }
}
