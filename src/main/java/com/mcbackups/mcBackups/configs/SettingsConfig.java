package com.mcbackups.mcBackups.configs;

import org.bukkit.plugin.java.JavaPlugin;
import services.plasma.pLibraries.util.config.creator.ConfigCreator;
import services.plasma.pLibraries.util.config.creator.annotation.Path;

import java.util.Arrays;
import java.util.List;

public class SettingsConfig extends ConfigCreator {

    public static SettingsConfig INSTANCE;

    @Path(id = "backup.enabled")
    public static boolean BACKUP_ENABLED = false;

    @Path(id = "backup.interval")
    public static String BACKUP_INTERVAL = "3h";

    @Path(id = "backup.folders")
    public static List<String> BACKUP_FOLDERS = Arrays.asList(
            "plugins",
            "world",
            "world_nether",
            "world_the_end"
    );

    @Path(id = "backup.ignored-folders")
    public static List<String> BACKUP_IGNORED_FOLDERS = Arrays.asList(
            "plugins/MCBackup",
            "plugins/CoreProtect",
            "plugins/squaremap",
            "plugins/dynmap"
    );

    @Path(id = "backup.provider.mcbackup.enabled")
    public static boolean BACKUP_PROVIDER_MCBACKUPS_ENABLED = true;

    @Path(id = "backup.provider.mcbackup.api-key")
    public static String BACKUP_PROVIDER_MCBACKUPS_API_KEY = "mcbkp_xxxxxxxxx";

    @Path(id = "backup.provider.googledrive.enabled")
    public static boolean BACKUP_PROVIDER_GOOGLEDRIVE_ENABLED = false;

    @Path(id = "backup.provider.googledrive.refresh-token")
    public static String BACKUP_PROVIDER_GOOGLEDRIVE_REFRESH_TOKEN = "this-will-automatically-generate";

    @Path(id = "backup.provider.sftp.enabled")
    public static boolean BACKUP_PROVIDER_SFTP_ENABLED = false;

    @Path(id = "backup.provider.sftp.host")
    public static String BACKUP_PROVIDER_SFTP_HOST = "sftp.example.com";

    @Path(id = "backup.provider.sftp.port")
    public static int BACKUP_PROVIDER_SFTP_PORT = 22;

    @Path(id = "backup.provider.sftp.host-fingerprint")
    public static String BACKUP_PROVIDER_SFTP_HOST_FINGERPRINT = "";

    @Path(id = "backup.provider.sftp.auth.username")
    public static String BACKUP_PROVIDER_SFTP_USERNAME = "username";

    @Path(id = "backup.provider.sftp.auth.password.enabled")
    public static boolean BACKUP_PROVIDER_SFTP_AUTH_PASSWORD_ENABLED = true;

    @Path(id = "backup.provider.sftp.auth.password.password")
    public static String BACKUP_PROVIDER_SFTP_AUTH_PASSWORD = "password";

    @Path(id = "backup.provider.sftp.auth.private-key.path")
    public static String BACKUP_PROVIDER_SFTP_AUTH_PRIVATE_KEY_PATH = "sftp/id_ed25519";

    @Path(id = "backup.provider.sftp.auth.private-key.passphrase")
    public static String BACKUP_PROVIDER_SFTP_AUTH_PRIVATE_KEY_PASSPHRASE = "";

    @Path(id = "backup.provider.sftp.remote-dir")
    public static String BACKUP_PROVIDER_SFTP_REMOTE_DIR = "/backups";

    public SettingsConfig(JavaPlugin plugin) {
        super(plugin, "settings.yml");
        INSTANCE = this;
    }

}
