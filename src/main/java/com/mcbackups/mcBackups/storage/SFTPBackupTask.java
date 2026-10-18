package com.mcbackups.mcBackups.storage;

import com.mcbackups.mcBackups.MCBackups;
import com.mcbackups.mcBackups.configs.SettingsConfig;
import com.mcbackups.mcBackups.utils.SFTPHostKeyVerifier;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.StreamCopier;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.password.PasswordUtils;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.xfer.TransferListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public class SFTPBackupTask extends BackupTask {

    public SFTPBackupTask() {
        super("SFTP");
    }

    @Override
    public void uploadBackup() throws IOException {

        Path zip = MCBackups.getInstance().getDataFolder().toPath().resolve("backups/" + zipArchiveName);
        try {
            Files.createDirectories(zip.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        createZipArchive(zip);

        String host = SettingsConfig.BACKUP_PROVIDER_SFTP_HOST;
        int port = SettingsConfig.BACKUP_PROVIDER_SFTP_PORT;
        String username = SettingsConfig.BACKUP_PROVIDER_SFTP_USERNAME;
        String password = SettingsConfig.BACKUP_PROVIDER_SFTP_AUTH_PASSWORD;
        String remoteDir = SettingsConfig.BACKUP_PROVIDER_SFTP_REMOTE_DIR;

        String privateKeyPath = SettingsConfig.BACKUP_PROVIDER_SFTP_AUTH_PRIVATE_KEY_PATH;
        String passphrase = SettingsConfig.BACKUP_PROVIDER_SFTP_AUTH_PRIVATE_KEY_PASSPHRASE;

        String remoteTemp = remoteDir + "/" + zip.getFileName() + ".part";
        String remoteFinal = remoteDir + "/" + zip.getFileName();

        try (SSHClient ssh = new SSHClient()) {
            ssh.setConnectTimeout(15_000);
            ssh.setTimeout(60_000);

            ssh.addHostKeyVerifier(new SFTPHostKeyVerifier(SettingsConfig.BACKUP_PROVIDER_SFTP_HOST_FINGERPRINT));

            ssh.connect(host, port);

            if(SettingsConfig.BACKUP_PROVIDER_SFTP_AUTH_PASSWORD_ENABLED) {
                ssh.authPassword(username, password);
            } else {
                Path keyPath = MCBackups.getInstance().getDataFolder().toPath().resolve(privateKeyPath);

                KeyProvider keys = passphrase == null || passphrase.isEmpty()
                        ? ssh.loadKeys(keyPath.toString())
                        : ssh.loadKeys(keyPath.toString(), PasswordUtils.createOneOff(passphrase.toCharArray()));

                ssh.authPublickey(username, keys);
            }

            try (SFTPClient sftp = ssh.newSFTPClient()) {
                sftp.mkdirs(remoteDir);

                sftp.getFileTransfer().setTransferListener(new TransferListener() {
                    @Override
                    public TransferListener directory(String name) {
                        return this;
                    }

                    @Override
                    public StreamCopier.Listener file(String name, long size) {
                        AtomicInteger lastLogged = new AtomicInteger();
                        return transferred -> {
                            int step = (int) (transferred * 100 / Math.max(size, 1)) / 10 * 10;
                            if (step > lastLogged.get()) {
                                lastLogged.set(step);
                                MCBackups.getInstance().getLogger().info("SFTP Backup: " + step + "%");
                            }
                        };
                    }
                });

                sftp.put(new FileSystemFile(zip.toFile()), remoteTemp);
                sftp.rename(remoteTemp, remoteFinal);
            }

            MCBackups.getInstance().getLogger().info("SFTP upload complete!");
        } finally {
            deleteZipArchive(zip);
        }

    }
}
