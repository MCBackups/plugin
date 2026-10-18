package net.mcbackup.mcBackup.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import net.mcbackup.mcBackup.MCBackup;
import net.mcbackup.mcBackup.authentication.DriveAuthentication;
import net.mcbackup.mcBackup.configs.LanguageConfig;
import net.mcbackup.mcBackup.configs.SettingsConfig;
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
        new DriveAuthentication().authenticate(sender);
    }

    @Subcommand("backup")
    @CommandPermission("mcbackup.command.mcbackup.backup")
    @Description("Create a backup of the server")
    public void backup(CommandSender sender) {
        sender.sendMessage(CC.translate("&aStarting backup..."));

        MCBackup.getInstance().getServer().getScheduler().runTaskAsynchronously(MCBackup.getInstance(), () -> {

            try {
                MCBackup.getBackupTask().uploadBackup();
            } catch (IOException e) {
                sender.sendMessage(CC.translateComponent("&cThere aws an error while creating the backup! Check console for more information."));
                throw new RuntimeException(e);
            }

        });

    }

}
