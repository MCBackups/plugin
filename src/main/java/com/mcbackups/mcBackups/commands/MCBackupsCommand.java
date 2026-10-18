package com.mcbackups.mcBackups.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import com.mcbackups.mcBackups.MCBackups;
import com.mcbackups.mcBackups.authentication.GoogleDriveAuthentication;
import com.mcbackups.mcBackups.configs.LanguageConfig;
import com.mcbackups.mcBackups.configs.SettingsConfig;
import com.mcbackups.mcBackups.storage.DriveBackupTask;
import org.bukkit.command.CommandSender;
import services.plasma.pLibraries.util.CC;

import java.io.IOException;

@CommandAlias("mcbackup|backup")
public class MCBackupsCommand extends BaseCommand {

    @HelpCommand
    @Default
    public void help(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("reload")
    @CommandPermission("mcbackup.command.mcbackup.reload")
    @Description("Reload all MCBackups configuration files")
    public void reload(CommandSender sender) {
        LanguageConfig.INSTANCE.reload();
        SettingsConfig.INSTANCE.reload();

        sender.sendMessage(CC.translate(LanguageConfig.COMMANDS_MCBACKUPS_RELOADED));
    }

    @Subcommand("drive auth")
    @CommandPermission("mcbackup.command.mcbackup.auth")
    @Description("Authenticate your Google Drive account")
    public void auth(CommandSender sender) {
        if(SettingsConfig.BACKUP_PROVIDER_GOOGLEDRIVE_ENABLED && MCBackups.getBackupTask() instanceof DriveBackupTask) {
            new GoogleDriveAuthentication().authenticate(sender);
        } else {
            sender.sendMessage(CC.translate("&cGoogle Drive is not enabled in the config."));
        }

    }

    @Subcommand("backup")
    @CommandPermission("mcbackup.command.mcbackup.backup")
    @Description("Create a backup of the server")
    public void backup(CommandSender sender) {

        MCBackups.getInstance().getServer().getScheduler().runTaskAsynchronously(MCBackups.getInstance(), () -> {

            if(MCBackups.getBackupTask() == null) {
                sender.sendMessage(CC.translateComponent("&cNo backup provider is enabled in the config."));
                return;
            }

            if(!SettingsConfig.BACKUP_ENABLED) {
                sender.sendMessage(CC.translateComponent("&cBackups are disabled in the config."));
                return;
            }

            try {
                MCBackups.getBackupTask().uploadBackup();
            } catch (IOException e) {
                sender.sendMessage(CC.translateComponent("&cThere was an error while creating the backup! Check console for more information."));
                throw new RuntimeException(e);
            }

        });

    }

}
