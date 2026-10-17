package net.mcbackup.mcBackup;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import net.mcbackup.mcBackup.commands.MCBackupCommand;
import net.mcbackup.mcBackup.configs.LanguageConfig;
import net.mcbackup.mcBackup.configs.SettingsConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public final class MCBackup extends JavaPlugin {
    @Getter public static MCBackup instance;
    @Getter public static PaperCommandManager commandManager;

    @Override
    public void onEnable() {
        instance = this;

        new LanguageConfig(this);
        new SettingsConfig(this);

        commandManager = new PaperCommandManager(this);
        commandManager.enableUnstableAPI("help");

        Arrays.asList(
                new MCBackupCommand()
        ).forEach(command -> commandManager.registerCommand(command, true));

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
