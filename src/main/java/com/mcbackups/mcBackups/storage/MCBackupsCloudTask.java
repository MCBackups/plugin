package com.mcbackups.mcBackups.storage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mcbackups.mcBackups.MCBackups;
import com.mcbackups.mcBackups.configs.SettingsConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MCBackupsCloudTask extends BackupTask {


    public MCBackupsCloudTask() {
        super("MCBackups Cloud");
    }

    public final String MCBACKUPS_CLOUD_URL = "https://mcbk.auri.lol";

    @Override
    public void uploadBackup() throws IOException {

        Path zip = MCBackups.getInstance().getDataFolder().toPath().resolve("backups/" + zipArchiveName);
        try {
            Files.createDirectories(zip.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        createZipArchive(zip);

        HttpRequest request = HttpRequest.newBuilder(URI.create(MCBACKUPS_CLOUD_URL + "/cloud/api/backups"))
                .timeout(Duration.ofMinutes(30))
                .header("Content-Type", "application/octet-stream")
                .header("X-Backup-Name", zipArchiveName)
                .header("Authorization", "Bearer " + SettingsConfig.BACKUP_PROVIDER_MCBACKUPS_API_KEY)
                .POST(progressPublisher(zip))
                .build();

        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        MCBackups.getInstance().getLogger().info("Uploading " + zipArchiveName + " (" + formatSize(Files.size(zip)) + ") to MCBackups Cloud...");

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (response.statusCode() == 201) {
            MCBackups.getInstance().getLogger().info("Upload complete!");
        } else {
            MCBackups.getInstance().getLogger().severe("Upload failed (HTTP " + response.statusCode() + "): " + extractError(response.body()));
        }

        deleteZipArchive(zip);

    }

    private HttpRequest.BodyPublisher progressPublisher(Path zip) throws IOException {
        long total = Math.max(Files.size(zip), 1);
        HttpRequest.BodyPublisher file = HttpRequest.BodyPublishers.ofFile(zip);

        Flow.Publisher<ByteBuffer> tracked = subscriber -> {
            AtomicLong sent = new AtomicLong();
            AtomicInteger lastLogged = new AtomicInteger();

            file.subscribe(new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscriber.onSubscribe(subscription);
                }

                @Override
                public void onNext(ByteBuffer item) {
                    int size = item.remaining();
                    subscriber.onNext(item);

                    int step = (int) (sent.addAndGet(size) * 100 / total) / 10 * 10;
                    if (step > lastLogged.get()) {
                        lastLogged.set(step);
                        MCBackups.getInstance().getLogger().info("Uploaded " + step + "%");
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    subscriber.onError(throwable);
                }

                @Override
                public void onComplete() {
                    subscriber.onComplete();
                }
            });
        };

        return HttpRequest.BodyPublishers.fromPublisher(tracked, total);
    }

    private String formatSize(long bytes) {
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private String extractError(String responseBody) {
        try {
            JsonObject body = JsonParser.parseString(responseBody).getAsJsonObject();
            if (body.has("error")) {
                return body.get("error").getAsString();
            }
        } catch (JsonParseException | IllegalStateException ignored) {
        }
        return responseBody.isBlank() ? "empty response" : responseBody;
    }

}
