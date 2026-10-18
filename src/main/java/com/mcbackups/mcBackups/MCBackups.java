package com.mcbackups.mcBackups;

import co.aikar.commands.PaperCommandManager;
import com.mcbackups.mcBackups.storage.SFTPBackupTask;
import lombok.Getter;
import com.mcbackups.mcBackups.commands.MCBackupsCommand;
import com.mcbackups.mcBackups.configs.LanguageConfig;
import com.mcbackups.mcBackups.configs.SettingsConfig;
import com.mcbackups.mcBackups.storage.BackupTask;
import com.mcbackups.mcBackups.storage.DriveBackupTask;
import com.mcbackups.mcBackups.storage.MCBackupsCloudTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public final class MCBackups extends JavaPlugin {
    @Getter public static MCBackups instance;
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
                new MCBackupsCommand()
        ).forEach(command -> commandManager.registerCommand(command, true));

        if(SettingsConfig.BACKUP_ENABLED) {

            if(SettingsConfig.BACKUP_PROVIDER_GOOGLEDRIVE_ENABLED) {
                try {
                    backupTask = new DriveBackupTask();
                } catch (GeneralSecurityException | IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if(SettingsConfig.BACKUP_PROVIDER_MCBACKUPS_ENABLED) {
                backupTask = new MCBackupsCloudTask();
            }

            if(SettingsConfig.BACKUP_PROVIDER_SFTP_ENABLED) {
                backupTask = new SFTPBackupTask();
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
