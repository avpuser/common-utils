package com.avpuser.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.avpuser.progress.EmptyProgressListener;
import com.avpuser.progress.ProgressListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandExecutor {

    private static final Logger logger = LogManager.getLogger(CommandExecutor.class);

    public static int exec(String[] command, ProgressListener listener) {
        String commandStr = String.join(" ", command);
        logger.info("Command: " + commandStr);

        Process process;
        try {
            process = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

        // Getting command output
        StreamGobbler inputStreamGobbler = new StreamGobbler(process.getInputStream(), listener);
        StreamGobbler errorStreamGobbler = new StreamGobbler(process.getErrorStream(), new EmptyProgressListener());

        Thread inputThread = new Thread(inputStreamGobbler);
        Thread errorThread = new Thread(errorStreamGobbler);

        inputThread.start();
        errorThread.start();

        // Wait for the process to complete
        int exitCode = 0;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

        try {
            inputThread.join();
            errorThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

        if (exitCode != 0) {
            logger.error("Process exited with code: " + exitCode);
        }
        return exitCode;
    }

    private static class StreamGobbler implements Runnable {

        private final InputStream inputStream;

        private final ProgressListener listener;

        private final Pattern progressPattern = Pattern.compile(
                "\\[download\\]\\s+(\\d+\\.\\d+)%"); // Regex for progress

        public StreamGobbler(InputStream inputStream, ProgressListener listener) {
            this.inputStream = inputStream;
            this.listener = listener;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info(line);

                    // Parse and handle progress if listener is provided
                    if (listener != null) {
                        Matcher matcher = progressPattern.matcher(line);
                        if (matcher.find()) {
                            double progress = Double.parseDouble(matcher.group(1));
                            listener.onProgress((int) progress); // Notify progress
                        }
                    }

                }
            } catch (IOException e) {
                logger.error("Error reading stream", e);
                throw new RuntimeException(e);
            }
        }
    }
}
