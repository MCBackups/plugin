package net.mcbackup.mcBackup.configs;

import org.bukkit.plugin.java.JavaPlugin;
import services.plasma.pLibraries.util.config.creator.ConfigCreator;
import services.plasma.pLibraries.util.config.creator.annotation.Path;

public class LanguageConfig extends ConfigCreator {

    public static LanguageConfig INSTANCE;

    @Path(id = "commands.mcbackup.reloaded")
    public static String COMMANDS_MCBACKUP_RELOADED = "&aMCBackup configuration files have been reloaded.";


    public LanguageConfig(JavaPlugin plugin) {
        super(plugin, "language.yml");
        INSTANCE = this;
    }

}
