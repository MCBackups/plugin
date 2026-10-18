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

    public File createZipArchive(List<Path> sourceDirs, Path zipFile) {

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


            List<String> folderNamesToBackup = new ArrayList<>(SettingsConfig.BACKUP_FOLDERS);
            List<String> folderNamesToExclude = new ArrayList<>(SettingsConfig.BACKUP_IGNORED_FOLDERS);


            for (String folderName : folderNamesToBackup) {
                Path serverRoot = MCBackup.getInstance().getServer().getWorldContainer().toPath().toAbsolutePath().normalize();
                Path folderPath = serverRoot.resolve(folderName);
                if (Files.exists(folderPath)) {
                    foldersToBackup.add(folderPath);
                } else {
                    MCBackup.getInstance().getLogger().warning("Folder " + folderName + " does not exist and will be skipped.");
                }
            }

            for(String folderName : folderNamesToExclude) {
                Path serverRoot = MCBackup.getInstance().getServer().getWorldContainer().toPath().toAbsolutePath().normalize();
                Path folderPath = serverRoot.resolve(folderName);
                if (Files.exists(folderPath)) {
                    foldersToExclude.add(folderPath);
                } else {
                    MCBackup.getInstance().getLogger().warning("Folder " + folderName + " does not exist.");
                }
            }


            Path zip = MCBackup.getInstance().getDataFolder().toPath().resolve("backups/" + zipArchiveName);
            try {
                Files.createDirectories(zip.getParent());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return zip.toFile();

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
