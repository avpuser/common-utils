package com.avpuser.utils;

import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ResourceFileUtils {

    public static String getTestResourceFilePath(Class<?> clazz, String fileName) {
        String path = clazz.getProtectionDomain().getCodeSource().getLocation().getPath().replace("target/test-classes", "src/test/java/resources");

        return path + fileName.replace("%20", " ");
    }


    public static String getFilePath(Class<?> clazz, String fileName) {
        String path = clazz.getProtectionDomain().getCodeSource().getLocation().getPath().replace("target/classes", "src/main/java");

        return (path + (clazz.getPackageName()).replace(".", "/") + "/" + fileName).replace("%20", " ");
    }

    public static String readTestFile(Class<?> clazz, String fileName) {
        String filePath = getTestResourceFilePath(clazz, fileName);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            for (String line; (line = br.readLine()) != null; ) {
                if (line.length() > 0) {
                    sb.append(line).append("\n");
                }
            }
            // line is not visible here.
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    public static String readFile(Class<?> clazz, String fileName) {
        String filePath = getTestResourceFilePath(clazz, fileName);
        if (FileUtils.fileExists(filePath)) {
            return readFromFile(clazz, fileName);
        }
        return readFileFromResource(fileName);
    }

    private static String readFromFile(Class<?> clazz, String fileName) {
        String filePath = getTestResourceFilePath(clazz, fileName);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            for (String line; (line = br.readLine()) != null; ) {
                if (!line.isEmpty()) {
                    sb.append(line).append("\n");
                }
            }
            // line is not visible here.
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    public static String readFileFromResource(String fileName) {
        StringBuilder sb = new StringBuilder();
        ClassPathResource resource = new ClassPathResource(fileName);
        try (InputStream inputStream = resource.getInputStream(); BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty()) {
                    sb.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading resource: " + fileName, e);
        }

        return sb.toString();
    }

}
