package com.avpuser.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ResourceFileUtils {

    private static final Logger logger = LogManager.getLogger(ResourceFileUtils.class);

    public static String resolveTestResourceFilePath(String fileName) {
        String resourceRoot = "src/test/resources/";
        String projectRoot = new File("").getAbsolutePath();
        String fullPath = projectRoot + File.separator + resourceRoot + fileName;
        return fullPath.replace("%20", " ");
    }

    public static String readTestResourceFileContent(String fileName) {
        String filePath = resolveTestResourceFilePath(fileName);
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

    public static String readResourceFileContent(String fileName) {
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
            logger.error("Error reading resource: " + fileName, e);
            throw new RuntimeException("Error reading resource: " + fileName, e);
        }

        return sb.toString();
    }
}
