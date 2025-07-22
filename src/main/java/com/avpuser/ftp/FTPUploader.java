package com.avpuser.ftp;

import com.avpuser.file.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;

public class FTPUploader {

    protected static final Logger logger = LogManager.getLogger(FTPUploader.class);

    private final String server;
    private final int port;
    private final String user;
    private final String pass;

    private final String directory;

    private final String httpPath;

    public FTPUploader(String server, int port, String user, String pass, String directory, String httpPath) {
        this.server = server;
        this.port = port;
        this.user = user;
        this.pass = pass;
        this.directory = directory;
        this.httpPath = httpPath;
    }

    public String uploadFile(String filePath) {
        if (!FileUtils.fileExists(filePath)) {
            throw new RuntimeException("File not exists: " + filePath);
        }

        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(server, port);
            ftpClient.login(user, pass);
            ftpClient.enterLocalPassiveMode();

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            // Path on the FTP server where the file will be uploaded
            String remoteFile = directory + FileUtils.getFileName(filePath);

            FileInputStream inputStream = new FileInputStream(filePath);

            System.out.println("Starting file download: " + filePath);
            boolean done = ftpClient.storeFile(remoteFile, inputStream);
            inputStream.close();
            if (done) {
                logger.info("File uploaded successfully.");
            }

            return httpPath + remoteFile;

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }
}
