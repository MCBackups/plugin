package net.mcbackup.mcBackup.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import net.mcbackup.mcBackup.MCBackup;
import net.mcbackup.mcBackup.authentication.DriveAuthentication;
import net.mcbackup.mcBackup.configs.LanguageConfig;
import net.mcbackup.mcBackup.configs.SettingsConfig;
import net.mcbackup.mcBackup.storage.DriveBackupTask;
import org.bukkit.command.CommandSender;
import services.plasma.pLibraries.util.CC;

import java.io.IOException;

@CommandAlias("mcbackup|backup")
public class MCBackupCommand extends BaseCommand {

    @HelpCommand
    @Default
    public void help(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("reload")
    @CommandPermission("mcbackup.command.mcbackup.reload")
    @Description("Reload all MCBackup configuration files")
    public void reload(CommandSender sender) {
        LanguageConfig.INSTANCE.reload();
        SettingsConfig.INSTANCE.reload();

        sender.sendMessage(CC.translate(LanguageConfig.COMMANDS_MCBACKUP_RELOADED));
    }

    @Subcommand("drive auth")
    @CommandPermission("mcbackup.command.mcbackup.auth")
    @Description("Authenticate your Google Drive account")
    public void auth(CommandSender sender) {
        if(SettingsConfig.BACKUP_PROVIDER_GOOGLEDRIVE_ENABLED && MCBackup.getBackupTask() instanceof DriveBackupTask) {
            new DriveAuthentication().authenticate(sender);
        } else {
            sender.sendMessage(CC.translate("&cGoogle Drive is not enabled in the config."));
        }

    }

    @Subcommand("backup")
    @CommandPermission("mcbackup.command.mcbackup.backup")
    @Description("Create a backup of the server")
    public void backup(CommandSender sender) {

        MCBackup.getInstance().getServer().getScheduler().runTaskAsynchronously(MCBackup.getInstance(), () -> {

            if(MCBackup.getBackupTask() == null) {
                sender.sendMessage(CC.translateComponent("&cNo backup provider is enabled in the config."));
                return;
            }

            if(!SettingsConfig.BACKUP_ENABLED) {
                sender.sendMessage(CC.translateComponent("&cBackups are disabled in the config."));
                return;
            }

            try {
                MCBackup.getBackupTask().uploadBackup();
            } catch (IOException e) {
                sender.sendMessage(CC.translateComponent("&cThere aws an error while creating the backup! Check console for more information."));
                throw new RuntimeException(e);
            }

        });

    }

}
