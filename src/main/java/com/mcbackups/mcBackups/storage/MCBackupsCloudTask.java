package com.mcbackups.mcBackups.storage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcbackups.mcBackups.MCBackups;
import com.mcbackups.mcBackups.configs.SettingsConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class MCBackupsCloudTask extends BackupTask {


    public MCBackupsCloudTask() {
        super("MCBackups Cloud");
    }

    public final String MCBACKUPS_CLOUD_URL = "https://api.mcbackup.net";

    @Override
    public void uploadBackup() throws IOException {

        Path zip = MCBackups.getInstance().getDataFolder().toPath().resolve("backups/" + zipArchiveName);
        try {
            Files.createDirectories(zip.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        createZipArchive(zip);

        HttpRequest request = HttpRequest.newBuilder(URI.create(MCBACKUPS_CLOUD_URL + "/backups"))
                .timeout(Duration.ofMinutes(30))
                .header("Content-Type", "application/octet-stream")
                .header("X-Backup-Name", zipArchiveName)
                .header("Authorization", "Bearer " + SettingsConfig.BACKUP_PROVIDER_MCBACKUPS_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofFile(zip))
                .build();

        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();

        if (response.statusCode() != 201) {
            String error = body.has("error") ? body.get("error").getAsString() : response.body();
            MCBackups.getInstance().getLogger().severe("Upload failed (HTTP " + response.statusCode() + "): " + error);
        }
    }

}
