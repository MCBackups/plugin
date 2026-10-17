package net.mcbackup.mcBackup.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import net.mcbackup.mcBackup.configs.LanguageConfig;
import net.mcbackup.mcBackup.configs.SettingsConfig;
import net.mcbackup.mcBackup.authentication.DriveAuthentication;
import org.bukkit.command.CommandSender;
import services.plasma.pLibraries.util.CC;

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

    @Subcommand("auth")
    @CommandPermission("mcbackup.command.mcbackup.auth")
    @Description("Authenticate your MCBackup account")
    public void auth(CommandSender sender) {
        new DriveAuthentication().authenticate(sender);
    }

}
