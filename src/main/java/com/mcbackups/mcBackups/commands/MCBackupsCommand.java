package com.mcbackups.mcBackups.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import com.mcbackups.mcBackups.MCBackups;
import com.mcbackups.mcBackups.authentication.GoogleDriveAuthentication;
import com.mcbackups.mcBackups.configs.LanguageConfig;
import com.mcbackups.mcBackups.configs.SettingsConfig;
import com.mcbackups.mcBackups.storage.DriveBackupTask;
import com.mcbackups.mcBackups.utils.SFTPHostKeyScanner;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.CommandSender;
import services.plasma.pLibraries.util.CC;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CommandAlias("mcbackups|backup")
public class MCBackupsCommand extends BaseCommand {

    private static final long PENDING_TRUST_EXPIRY_MILLIS = 120_000L;

    private final Map<String, PendingTrust> pendingTrusts = new ConcurrentHashMap<>();

    private record PendingTrust(String host, int port, String fingerprint, long expiresAt) {
    }

    @HelpCommand
    @Default
    public void help(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("reload")
    @CommandPermission("mcbackups.command.mcbackups.reload")
    @Description("Reload all MCBackups configuration files")
    public void reload(CommandSender sender) {
        LanguageConfig.INSTANCE.reload();
        SettingsConfig.INSTANCE.reload();

        sender.sendMessage(CC.translate(LanguageConfig.COMMANDS_MCBACKUPS_RELOADED));
    }

    @Subcommand("drive auth")
    @CommandPermission("mcbackups.command.mcbackups.auth")
    @Description("Authenticate your Google Drive account")
    public void auth(CommandSender sender) {
        if(SettingsConfig.BACKUP_PROVIDER_GOOGLEDRIVE_ENABLED && MCBackups.getBackupTask() instanceof DriveBackupTask) {
            new GoogleDriveAuthentication().authenticate(sender);
        } else {
            sender.sendMessage(CC.translate("&cGoogle Drive is not enabled in the config."));
        }

    }

    @Subcommand("sftp trust")
    @CommandPermission("mcbackups.command.mcbackups.sftp.trust")
    @Description("Fetch and trust the SFTP server's host key")
    public void sftpTrust(CommandSender sender) {
        String host = SettingsConfig.BACKUP_PROVIDER_SFTP_HOST;
        int port = SettingsConfig.BACKUP_PROVIDER_SFTP_PORT;

        sender.sendMessage(CC.translateComponent("&7Fetching host key from &f" + host + ":" + port + "&7..."));

        MCBackups.getInstance().getServer().getScheduler().runTaskAsynchronously(MCBackups.getInstance(), () -> {
            String fingerprint;
            try {
                fingerprint = SFTPHostKeyScanner.fetchFingerprint(host, port);
            } catch (IOException e) {
                sender.sendMessage(CC.translateComponent("&cCould not fetch the host key from " + host + ":" + port + ": " + e.getMessage()));
                return;
            }

            pendingTrusts.put(sender.getName(), new PendingTrust(host, port, fingerprint, System.currentTimeMillis() + PENDING_TRUST_EXPIRY_MILLIS));

            Component buttons = CC.translateComponent("&a&l[TRUST]").clickEvent(ClickEvent.runCommand("/mcbackup sftp confirm"))
                    .append(Component.text("  "))
                    .append(CC.translateComponent("&c&l[CANCEL]").clickEvent(ClickEvent.runCommand("/mcbackup sftp cancel")));

            sender.sendMessage(CC.translateComponent("&eThe SFTP server &f" + host + ":" + port + " &epresented this host key:"));
            sender.sendMessage(CC.translateComponent("&f" + fingerprint));
            sender.sendMessage(CC.translateComponent("&7Only trust it if it matches your server. Run &f/mcbackup sftp confirm &7or click:"));
            sender.sendMessage(buttons);
        });
    }

    @Subcommand("sftp confirm")
    @CommandPermission("mcbackups.command.mcbackups.sftp.trust")
    @Description("Confirm trusting the fetched SFTP host key")
    public void sftpConfirm(CommandSender sender) {
        PendingTrust pending = pendingTrusts.remove(sender.getName());

        if (pending == null || pending.expiresAt() < System.currentTimeMillis()) {
            sender.sendMessage(CC.translateComponent("&cThere is no pending host key to trust. Run /mcbackup sftp trust first."));
            return;
        }

        if (!pending.host().equals(SettingsConfig.BACKUP_PROVIDER_SFTP_HOST) || pending.port() != SettingsConfig.BACKUP_PROVIDER_SFTP_PORT) {
            sender.sendMessage(CC.translateComponent("&cThe SFTP host changed since the key was fetched. Run /mcbackup sftp trust again."));
            return;
        }

        SettingsConfig.INSTANCE.getConfig().set("backup.provider.sftp.host-fingerprint", pending.fingerprint());
        SettingsConfig.INSTANCE.getConfig().save();
        SettingsConfig.BACKUP_PROVIDER_SFTP_HOST_FINGERPRINT = pending.fingerprint();

        sender.sendMessage(CC.translateComponent("&aNow trusting &f" + pending.fingerprint() + " &afor " + pending.host() + ":" + pending.port() + "."));
    }

    @Subcommand("sftp cancel")
    @CommandPermission("mcbackups.command.mcbackups.sftp.trust")
    @Description("Cancel trusting the fetched SFTP host key")
    public void sftpCancel(CommandSender sender) {
        if (pendingTrusts.remove(sender.getName()) != null) {
            sender.sendMessage(CC.translateComponent("&7Host key was not trusted."));
        } else {
            sender.sendMessage(CC.translateComponent("&cThere is no pending host key."));
        }
    }

    @Subcommand("backup")
    @CommandPermission("mcbackups.command.mcbackups.backup")
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
