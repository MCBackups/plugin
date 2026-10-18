package net.mcbackup.mcBackup.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.mcbackup.mcBackup.MCBackup;
import net.mcbackup.mcBackup.configs.SettingsConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@AllArgsConstructor
@Getter
@Setter
public abstract class BackupTask {

    private String name;

    public final String zipArchiveName = "MCBackup_" + System.currentTimeMillis() + ".zip";
    public final List<Path> foldersToBackup = new ArrayList<>();
    public final List<Path> foldersToExclude = new ArrayList<>();

    public File createZipArchive(Path zipFile) {

        Path zipAbsolute = zipFile.toAbsolutePath();
        Path serverRoot = MCBackup.getInstance().getServer().getWorldContainer().toPath().toAbsolutePath().normalize();

        foldersToBackup.clear();
        foldersToExclude.clear();

        for (String folderName : new ArrayList<>(SettingsConfig.BACKUP_FOLDERS)) {
            Path folderPath = serverRoot.resolve(folderName).normalize();
            if (Files.exists(folderPath)) {
                foldersToBackup.add(folderPath);
            } else {
                MCBackup.getInstance().getLogger().warning("Folder " + folderName + " does not exist and will be skipped.");
            }
        }

        for (String folderName : new ArrayList<>(SettingsConfig.BACKUP_IGNORED_FOLDERS)) {
            Path folderPath = serverRoot.resolve(folderName).normalize();
            if (Files.exists(folderPath)) {
                foldersToExclude.add(folderPath);
            } else {
                MCBackup.getInstance().getLogger().warning("Folder " + folderName + " does not exist.");
            }
        }

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            zos.setLevel(Deflater.BEST_SPEED);

            for (Path sourceDir : foldersToBackup) {
                Path base = sourceDir.getParent();

                Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (foldersToExclude.contains(dir.toAbsolutePath().normalize())) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

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

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        MCBackup.getInstance().getLogger().warning("Skipping unreadable file " + file + ": " + exc.getMessage());
                        return FileVisitResult.CONTINUE;
                    }
                });
            }

            return zipFile.toFile();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteZipArchive(Path zipFile) {
        try {
            Files.deleteIfExists(zipFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract void uploadBackup() throws IOException;

}
