package net.mcbackup.mcBackup.storage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mcbackup.mcBackup.MCBackup;
import net.mcbackup.mcBackup.configs.SettingsConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class MCBackupCloudTask extends BackupTask {


    public MCBackupCloudTask() {
        super("MCBackup Cloud");
    }

    public final String MCBACKUP_CLOUD_URL = "https://api.mcbackup.net";

    @Override
    public void uploadBackup() throws IOException {

        Path zip = MCBackup.getInstance().getDataFolder().toPath().resolve("backups/" + zipArchiveName);
        try {
            Files.createDirectories(zip.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        createZipArchive(zip);

        HttpRequest request = HttpRequest.newBuilder(URI.create(MCBACKUP_CLOUD_URL + "/backups"))
                .timeout(Duration.ofMinutes(30))
                .header("Content-Type", "application/octet-stream")
                .header("X-Backup-Name", zipArchiveName)
                .header("Authorization", "Bearer " + SettingsConfig.BACKUP_PROVIDER_MCBACKUP_API_KEY)
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
            MCBackup.getInstance().getLogger().severe("Upload failed (HTTP " + response.statusCode() + "): " + error);
        }
    }

}
