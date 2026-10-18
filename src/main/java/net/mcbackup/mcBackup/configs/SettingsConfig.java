package net.mcbackup.mcBackup.configs;

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
    public static boolean BACKUP_PROVIDER_MCBACKUP_ENABLED = true;

    @Path(id = "backup.provider.mcbackup.api-key")
    public static String BACKUP_PROVIDER_MCBACKUP_API_KEY = "mcbkp_xxxxxxxxx";

    @Path(id = "backup.provider.googledrive.enabled")
    public static boolean BACKUP_PROVIDER_GOOGLEDRIVE_ENABLED = false;

    @Path(id = "backup.provider.googledrive.refresh-token")
    public static String BACKUP_PROVIDER_GOOGLEDRIVE_REFRESH_TOKEN = "this-will-automatically-generate";



    public SettingsConfig(JavaPlugin plugin) {
        super(plugin, "settings.yml");
        INSTANCE = this;
    }

}
