package net.mcbackup.mcBackup.storage;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import net.mcbackup.mcBackup.MCBackup;
import net.mcbackup.mcBackup.authentication.DriveAuthentication;
import net.mcbackup.mcBackup.configs.SettingsConfig;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DriveBackupTask extends BackupTask {
    public Drive drive = buildDrive();

    public DriveBackupTask() throws GeneralSecurityException, IOException {
        super("Google Drive");
    }

    private Drive buildDrive() throws IOException, GeneralSecurityException {
        String refreshToken = SettingsConfig.BACKUP_PROVIDER_GOOGLEDRIVE_REFRESH_TOKEN;
        if (refreshToken == null) {
            throw new IllegalStateException("Google Drive is not linked, run the link command first.");
        }

        UserCredentials creds = UserCredentials.newBuilder()
                .setClientId(DriveAuthentication.CLIENT_ID)
                .setClientSecret(DriveAuthentication.CLIENT_SECRET)
                .setRefreshToken(refreshToken)
                .build();

        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(creds))
                .setApplicationName("MCBackup")
                .build();
    }


    public String findOrCreateFolder() throws IOException {
        String folderMime = "application/vnd.google-apps.folder";
        List<File> found = drive.files().list()
                .setQ("name = 'MCBackup' and mimeType = '" + folderMime + "' and trashed = false")
                .setFields("files(id)").execute().getFiles();

        return !found.isEmpty() ? found.get(0).getId()
                : drive.files().create(new File().setName("MCBackup").setMimeType(folderMime))
                  .setFields("id").execute().getId();
    }


    @Override
    public void uploadBackup() throws IOException {

        Path zip = MCBackup.getInstance().getDataFolder().toPath().resolve("backups/" + zipArchiveName);
        try {
            Files.createDirectories(zip.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        createZipArchive(foldersToBackup, zip);

        File meta = new File().setName(zip.getFileName().toString()).setParents(List.of(findOrCreateFolder()));
        Drive.Files.Create create = drive.files().create(meta, new FileContent("application/zip", zip.toFile()))
                .setFields("id");
        MediaHttpUploader uploader = create.getMediaHttpUploader();
        uploader.setDirectUploadEnabled(false);
        uploader.setChunkSize(MediaHttpUploader.MINIMUM_CHUNK_SIZE * 40);
        uploader.setProgressListener(u -> {
            MCBackup.getInstance().getLogger().info("Uploaded " + (int) (u.getProgress() * 100) + "%");

            if(u.getProgress() == 1.0) {
                MCBackup.getInstance().getLogger().info("Upload complete!");
                deleteZipArchive(zip);
            }

        });

        create.execute();

    }

}
