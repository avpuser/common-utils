package com.avpuser.utils;

import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ResourceFileUtils {

    public static String getTestResourceFilePath(String fileName) {
        String resourceRoot = "src/test/resources/";
        String projectRoot = new File("").getAbsolutePath();
        String fullPath = projectRoot + File.separator + resourceRoot + fileName;

        return fullPath.replace("%20", " ");
    }

    public static String getFilePath(Class<?> clazz, String fileName) {
        String path = clazz.getProtectionDomain().getCodeSource().getLocation().getPath().replace("target/classes", "src/main/java");

        return (path + (clazz.getPackageName()).replace(".", "/") + "/" + fileName).replace("%20", " ");
    }

    public static String readTestFile(String fileName) {
        String filePath = getTestResourceFilePath(fileName);
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

    public static String readFile(String fileName) {
        String filePath = getTestResourceFilePath(fileName);
        if (FileUtils.fileExists(filePath)) {
            return readFromFile(fileName);
        }
        return readFileFromResource(fileName);
    }

    private static String readFromFile(String fileName) {
        String filePath = getTestResourceFilePath(fileName);
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

    public static String getResourceFilePath(String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            return resource.getFile().getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Unable to resolve resource path: " + resourcePath, e);
        }
    }

}
