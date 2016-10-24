package com.jd;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.Channel;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.*;

import java.io.*;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;

/**
 * @author: Max.Yurin
 */
public class DriveServiceTask extends TimerTask {
    public static final String EXPORT_FOLDER = "./../webapps/export/";
    /**
     * Application name.
     */
    private final String APPLICATION_NAME =
            "Drive API Java Quickstart";

    /**
     * Directory to store user credentials for this application.
     */
    private final java.io.File DATA_STORE_DIR = new java.io.File(
            System.getProperty("user.home"), ".credentials/drive-java-quickstart");

    /**
     * Global instance of the {@link FileDataStoreFactory}.
     */
    private FileDataStoreFactory DATA_STORE_FACTORY;

    /**
     * Global instance of the JSON factory.
     */
    private final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();

    /**
     * Global instance of the HTTP transport.
     */
    private HttpTransport HTTP_TRANSPORT;

    /**
     * Global instance of the scopes required by this quickstart.
     * <p>
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/drive-java-quickstart
     */
    private final List<String> SCOPES =
//            new ArrayList<>(DriveScopes.all());
            Arrays.asList(DriveScopes.DRIVE);

    {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     *
     * @return an authorized Credential object.
     * @throws IOException
     */
    public Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in =
                DriveServiceTask.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build();
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");
        System.out.println(
                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    /**
     * Build and return an authorized Drive client service.
     *
     * @return an authorized Drive client service
     * @throws IOException
     */
    public Drive getDriveService() throws IOException {
        Credential credential = authorize();
        return new Drive.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    @Override
    public void run() {
        // Build a new authorized API client service.
        try {
            Drive service = getDriveService();

            cleanExportFolder();
            Thread.sleep(1000);

//            List<File> folderNames = getAllFolders();
//            for (File folder : folderNames) {
            long starttime = System.currentTimeMillis();
            exportFilesTofolder(service, "0B8y1AWfQuqCBMHJhTmxxSkNXR2c", "Test");
            System.out.println("Time elapsed: " + (System.currentTimeMillis() - starttime + "ms"));
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cleanExportFolder() throws IOException {
        System.out.println("The export directory is cleaned");
        FileUtils.cleanDirectory(new java.io.File(EXPORT_FOLDER));
    }

    private List<File> getAllFolders() throws IOException {
        List<File> folders = new ArrayList<>();
        Drive.Files.List request = getDriveService().files().list();
        FileList fileList = null;
        System.out.println("Loading folders info...");
        try {
            fileList = request.setPageSize(100)
                    .setCorpus("user")
                    .setSpaces("drive")
                    .setQ("'root' in parents" +
                            " and trashed = false" +
                            " and mimeType = 'application/vnd.google-apps.folder'")
                    .setFields("nextPageToken, files(id, name, parents)")
                    .execute();
            request.setPageToken(fileList.getNextPageToken());
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<File> files = fileList.getFiles();
        if (files == null || files.size() == 0) {
            System.out.println("No files found.");
        } else {
            for (File file : files) {
                printFileInfo(file);
                folders.add(file);
            }
        }
        return folders;
    }

    private void exportFilesTofolder(Drive service, String folderId, String folderName) throws IOException {
        // Print the names and IDs for up to 10 files.
        java.io.File exportFolder = new java.io.File(EXPORT_FOLDER + folderName);
        boolean mkdirs = exportFolder.mkdirs();
        System.out.println("Loading files to folder: [" + folderName + "] ...");

        Drive.Files.List request = service.files().list();
        do {
            FileList fileList = null;
            try {
                fileList = request.setPageSize(500)
                        .setCorpus("user")
                        .setSpaces("drive")
//                                        .setQ("'root' in parents" +
                        .setQ("'" + folderId + "' in parents" +
//                                                        " and mimeType = 'application/vnd.google-apps.file'" +
                                //                        " and mimeType = 'application/vnd.google-apps.folder'" +
                                " and mimeType = 'application/vnd.google-apps.spreadsheet'" +
                                " and trashed = false")
                        //                .setQ("'18iUh1dRREGiCMhw2_ASnzf-rGkaKoU97lTyYaE4jCKw' in parents")
                        .setFields("nextPageToken, files(id, name, mimeType)")
                        .execute();
                request.setPageToken(fileList.getNextPageToken());
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (fileList == null || fileList.size() == 0) {
                System.out.println("No files found.");
            } else {
                List<File> files = fileList.getFiles();
                System.out.println("Files: " + files.size());
                for (File file : fileList.getFiles()) {
                    exportFile(service, exportFolder, file);
                }
            }
        } while (request.getPageToken() != null && request.getPageToken().length() > 0);
    }

    private void exportFile(Drive service, java.io.File exportFolder, File file) {
        try {
            Drive.Files.Export export = service.files().export(file.getId(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            export.executeAndDownloadTo(baos);
            System.out.println("File:" + file.getName() + " downloaded");

            java.io.File exportFile = new java.io.File(exportFolder, file.getName() + ".xlsx");
            try(FileOutputStream bos = new FileOutputStream(exportFile)) {
                bos.write(baos.toByteArray());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printFileInfo(File file) {
        System.out.printf("Id: %s \n", file.getId());
        System.out.printf("\t Name: %s \n", file.getName());
        System.out.printf("\t MimeType: %s \n\n", file.getMimeType());
    }
}
