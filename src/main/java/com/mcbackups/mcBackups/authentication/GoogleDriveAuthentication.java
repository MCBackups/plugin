package com.mcbackups.mcBackups.authentication;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcbackups.mcBackups.MCBackups;
import com.mcbackups.mcBackups.configs.SettingsConfig;
import org.bukkit.command.CommandSender;
import services.plasma.pLibraries.util.CC;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

public class GoogleDriveAuthentication {

    public static final String CLIENT_ID = "201215840641-lc69ah0tq54ke9bn2i58oe9tk84p4r4u.apps.googleusercontent.com";
    public static final String CLIENT_SECRET = "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";
    private static final String SCOPE = "https://www.googleapis.com/auth/drive.file";
    private final HttpClient httpClient = HttpClient.newHttpClient();


    public void authenticate(CommandSender sender) {
        JsonObject code;
        try {
            code = post("https://oauth2.googleapis.com/device/code",
                    Map.of("client_id", CLIENT_ID, "scope", SCOPE));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        long expiresAt = System.currentTimeMillis() + code.get("expires_in").getAsLong() * 1000L;
        schedulePoll(code.get("device_code").getAsString(), code.get("interval").getAsLong(), expiresAt, sender);

        sender.sendMessage("Please visit " + code.get("verification_url").getAsString() + " and enter the code: " + code.get("user_code").getAsString());
    }


    private void schedulePoll(String deviceCode, long intervalSeconds, long expiresAt, CommandSender sender) {

        MCBackups.getInstance().getServer().getScheduler().runTaskLaterAsynchronously(MCBackups.getInstance(), () -> {
            if (System.currentTimeMillis() > expiresAt) {
               sender.sendMessage(CC.translateComponent("&cDevice code expired... You'll need to authenticate again"));
                return;
            }

            JsonObject res;
            try {
                res = post("https://oauth2.googleapis.com/token", Map.of(
                        "client_id", CLIENT_ID,
                        "client_secret", CLIENT_SECRET,
                        "device_code", deviceCode,
                        "grant_type", "urn:ietf:params:oauth:grant-type:device_code"));
            } catch (IOException e) {
                sender.sendMessage(CC.translateComponent("Token request failed" + e.getMessage()));
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if (res.has("refresh_token")) {
                saveRefreshToken(res.get("refresh_token").getAsString());
                sender.sendMessage(CC.translateComponent("&aGoogle Drive successfully linked!"));
                return;
            }

            String error = res.has("error") ? res.get("error").getAsString() : "unknown";
            switch (error) {
                case "authorization_pending" -> schedulePoll(deviceCode, intervalSeconds, expiresAt, sender);
                case "slow_down" -> schedulePoll(deviceCode, intervalSeconds + 5, expiresAt, sender);
                case "access_denied" -> sender.sendMessage(CC.translateComponent("&cGoogle Drive authentication failed: You denied the OAuth application."));
                case "expired_token" -> sender.sendMessage(CC.translateComponent("&cGoogle Drive authentication failed: Device code expired... You'll need to authenticate again"));
                default -> sender.sendMessage(CC.translateComponent("&cGoogle Drive authentication failed: " + res));
            }
        }, intervalSeconds * 20L);
    }

    private JsonObject post(String url, Map<String, String> form) throws Exception {
        String body = form.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return JsonParser.parseString(httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body()).getAsJsonObject();
    }

    private void saveRefreshToken(String refreshToken) {
        SettingsConfig.INSTANCE.getConfig().set("backup.provider.googledrive.refresh-token", refreshToken);
        SettingsConfig.INSTANCE.getConfig().save();
        SettingsConfig.INSTANCE.getConfig().reload();
    }

}
