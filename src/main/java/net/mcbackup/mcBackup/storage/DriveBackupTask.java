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

    public void createZipArchive(List<Path> sourceDirs, Path zipFile) {

        Path zipAbsolute = zipFile.toAbsolutePath();

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            zos.setLevel(Deflater.BEST_SPEED);

            for (Path sourceDir : sourceDirs) {
                Path base = sourceDir.toAbsolutePath().getParent();

                Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (file.getFileName().toString().equals("session.lock") || file.toAbsolutePath().equals(zipAbsolute)) {
                            return FileVisitResult.CONTINUE;
                        }

                        String entryName = base.relativize(file.toAbsolutePath()).toString().replace('\\', '/');

                        ZipEntry entry = new ZipEntry(entryName);
                        entry.setTime(attrs.lastModifiedTime().toMillis());
                        zos.putNextEntry(entry);
                        Files.copy(file, zos);
                        zos.closeEntry();

                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void uploadBackup() throws IOException {
        List<String> folderNamesToBackup = new ArrayList<>(SettingsConfig.BACKUP_FOLDERS);

        List<Path> foldersToBackup = new ArrayList<>();
        for (String folderName : folderNamesToBackup) {
            Path serverRoot = MCBackup.getInstance().getServer().getWorldContainer().toPath().toAbsolutePath().normalize();
            Path folderPath = serverRoot.resolve(folderName);
            if (Files.exists(folderPath)) {
                foldersToBackup.add(folderPath);
            } else {
                MCBackup.getInstance().getLogger().warning("Folder " + folderName + " does not exist and will be skipped.");
            }
        }

        String zipArchiveName = "MCBackup_" + System.currentTimeMillis() + ".zip";

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
        uploader.setProgressListener(u -> MCBackup.getInstance().getLogger().info("Upload " + (int) (u.getProgress() * 100) + "%"));
        String fileId = create.execute().getId();
    }

}
