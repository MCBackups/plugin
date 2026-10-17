package net.mcbackup.mcBackup.configs;

import org.bukkit.plugin.java.JavaPlugin;
import services.plasma.pLibraries.util.config.creator.ConfigCreator;
import services.plasma.pLibraries.util.config.creator.annotation.Path;

public class SettingsConfig extends ConfigCreator {

    public static SettingsConfig INSTANCE;

    @Path(id = "backup.enabled")
    public static boolean BACKUP_ENABLED = false;

    @Path(id = "backup.interval")
    public static String BACKUP_INTERVAL = "3h";

    @Path(id = "backup.provider.mcbackup.enabled")
    public static String BACKUP_PROVIDER_MCBACKUP_ENABLED = "true";

    @Path(id = "backup.provider.mcbackup.url")
    public static String BACKUP_PROVIDER_MCBACKUP_URL = "https://api.mcbackup.net";

    @Path(id = "backup.provider.mcbackup.api-key")
    public static String BACKUP_STORAGE_USERNAME = "mcbkp_xxxxxxxxx";


    public SettingsConfig(JavaPlugin plugin) {
        super(plugin, "settings.yml");
        INSTANCE = this;
    }

}
