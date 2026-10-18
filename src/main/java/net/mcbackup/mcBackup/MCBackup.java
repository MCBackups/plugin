package net.mcbackup.mcBackup;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import net.mcbackup.mcBackup.commands.MCBackupCommand;
import net.mcbackup.mcBackup.configs.LanguageConfig;
import net.mcbackup.mcBackup.configs.SettingsConfig;
import net.mcbackup.mcBackup.storage.BackupTask;
import net.mcbackup.mcBackup.storage.DriveBackupTask;
import net.mcbackup.mcBackup.storage.MCBackupCloudTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public final class MCBackup extends JavaPlugin {
    @Getter public static MCBackup instance;
    @Getter public static PaperCommandManager commandManager;
    @Getter public static BackupTask backupTask;

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

        if(SettingsConfig.BACKUP_ENABLED) {

            if(SettingsConfig.BACKUP_PROVIDER_GOOGLEDRIVE_ENABLED) {
                try {
                    backupTask = new DriveBackupTask();
                } catch (GeneralSecurityException | IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if(SettingsConfig.BACKUP_PROVIDER_MCBACKUP_ENABLED) {
                backupTask = new MCBackupCloudTask();
            }

            if(backupTask == null) {
                getLogger().warning("No backup provider is enabled in the config.");
            } else {
                getLogger().info("Backup task initialized with provider: " + backupTask.getName());
            }

        } else {
            getLogger().warning("Backups are disabled in the config.");
        }


    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }
}
