package com.avpuser.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    private static final Logger logger = LogManager.getLogger(FileUtils.class);

    private final static int MAX_FILE_NAME_LENGTH = 150;

    public static long fileSize(String filePath) {
        File file = new File(filePath);
        return file.exists() ? file.length() : 0;
    }

    public static List<String> searchFiles(File folder, String mask) {
        File[] files = folder.listFiles();
        List<String> res = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().contains(mask)) {
                    res.add(file.getAbsolutePath());
                }
            }
        }
        return res;
    }

    public static String getFileNameWithoutExtension(String inputFilePath) {
        File file = new File(inputFilePath);
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    public static boolean fileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    public static String getFileName(String filePath) {
        return new File(filePath).getName();
    }

    public static boolean fileDelete(String filePath) {
        File file = new File(filePath);
        return file.delete();
    }

    public static void deleteDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        if (directory.exists()) {
            try {
                org.apache.commons.io.FileUtils.deleteDirectory(directory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            logger.info("Directory and all its contents have been successfully deleted: " + directory.getAbsolutePath());
        } else {
            logger.info("The specified path does not exist: " + directory.getAbsolutePath());
        }
    }


    public static void cleanDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        if (directory.exists()) {
            try {
                org.apache.commons.io.FileUtils.deleteDirectory(directory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            logger.info("All contents of directory have been successfully deleted: " + directoryPath);
        } else {
            logger.info("The specified path does not exist: " + directoryPath);
        }
    }

    public static boolean createDirectoriesIfNotExist(String file) {
        Path targetPath = Paths.get(file);

        if (Files.exists(targetPath.getParent())) {
            return false;
        }

        try {
            Files.createDirectories(targetPath.getParent());
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean copyFile(String fromFilePath, String toFilePath) {
        Path sourcePath = Paths.get(fromFilePath);

        // Path to the target file (including the file name in the target directory)
        Path targetPath = Paths.get(toFilePath);

        createDirectoriesIfNotExist(toFilePath);

        try {
            // Copying a file, replacing an existing file and copying attributes
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);

            logger.debug("File copied successfully.");
        } catch (Exception e) {
            logger.error("Error copying file: " + e.getMessage(), e);
            return false;
        }
        return true;
    }

    public static boolean saveToFile(String content, String filePath) {
        Path path = Paths.get(filePath);

        if (Files.exists(path)) {
            return false;
        }

        // Create all the necessary directories, if they do not exist
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Create a file if it does not exist
        if (!Files.exists(path)) {
            try {
                Files.createFile(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        FileWriter writer;
        try {
            writer = new FileWriter(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (BufferedWriter bufferWriter = new BufferedWriter(writer)) {
            bufferWriter.write(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public static String sanitizeFileName(String russianTitle, FileNameType nameType) {
        if (russianTitle == null || russianTitle.isBlank()) {
            return "empty_file_name";
        }

        // Transliteration of Russian characters into English
        if (nameType.equals(FileNameType.TRANSLITERATE)) {
            russianTitle = transliterate(russianTitle).trim();
        }
        russianTitle = russianTitle.trim();

        // Replace spaces with underscores
        russianTitle = russianTitle.replaceAll(" ", "_");

        // remove special characters and spaces, leaving only letters and numbers
        String sanitizedTitle = russianTitle.replaceAll("[^a-zA-Zа-яА-Я0-9_]", "");

        // Limiting the length of the file name
        if (sanitizedTitle.length() > MAX_FILE_NAME_LENGTH) {
            sanitizedTitle = sanitizedTitle.substring(0, MAX_FILE_NAME_LENGTH);
        }

        // Remove the underscore at the end of the line if it is there
        if (sanitizedTitle.endsWith("_")) {
            sanitizedTitle = sanitizedTitle.substring(0, sanitizedTitle.length() - 1);
        }

        return sanitizedTitle;
    }

    private static String transliterate(String russianText) {
        String[][] alphabet = {{"а", "a"}, {"б", "b"}, {"в", "v"}, {"г", "g"}, {"д", "d"}, {"е", "e"},
                {"ё", "yo"}, {"ж", "zh"}, {"з", "z"}, {"и", "i"}, {"й", "y"}, {"к", "k"},
                {"л", "l"}, {"м", "m"}, {"н", "n"}, {"о", "o"}, {"п", "p"}, {"р", "r"}, {"с", "s"},
                {"т", "t"}, {"у", "u"}, {"ф", "f"}, {"х", "h"}, {"ц", "c"}, {"ч", "ch"}, {"ш", "sh"},
                {"щ", "sch"}, {"ъ", ""}, {"ы", "y"}, {"ь", ""}, {"э", "e"}, {"ю", "yu"}, {"я", "ya"},
                {"А", "A"}, {"Б", "B"}, {"В", "V"}, {"Г", "G"}, {"Д", "D"}, {"Е", "E"}, {"Ё", "Yo"},
                {"Ж", "Zh"}, {"З", "Z"}, {"И", "I"}, {"Й", "Y"}, {"К", "K"}, {"Л", "L"}, {"М", "M"},
                {"Н", "N"}, {"О", "O"}, {"П", "P"}, {"Р", "R"}, {"С", "S"}, {"Т", "T"}, {"У", "U"},
                {"Ф", "F"}, {"Х", "H"}, {"Ц", "C"}, {"Ч", "Ch"}, {"Ш", "Sh"}, {"Щ", "Sch"},
                {"Ъ", ""}, {"Ы", "Y"}, {"Ь", ""}, {"Э", "E"}, {"Ю", "Yu"}, {"Я", "Ya"}};


        // Transliteration of Russian letters into English
        for (String[] pair : alphabet) {
            russianText = russianText.replace(pair[0], pair[1]);
        }

        return russianText;
    }

    public static String computeSHA256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

}
