package com.mcbackups.mcBackups.utils;

import com.mcbackups.mcBackups.MCBackups;
import net.schmizz.sshj.common.Buffer;
import net.schmizz.sshj.common.SSHRuntimeException;
import net.schmizz.sshj.transport.verification.FingerprintVerifier;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class SFTPHostKeyVerifier implements HostKeyVerifier {

    private static final String SETTING_PATH = "backup.provider.sftp.host-fingerprint";

    private final String expectedFingerprint;
    private final HostKeyVerifier delegate;

    public SFTPHostKeyVerifier(String expectedFingerprint) {
        this.expectedFingerprint = expectedFingerprint == null ? "" : expectedFingerprint.trim();
        this.delegate = createDelegate(this.expectedFingerprint);
    }

    private HostKeyVerifier createDelegate(String fingerprint) {
        if (fingerprint.isEmpty()) {
            return null;
        }

        try {
            return FingerprintVerifier.getInstance(fingerprint);
        } catch (SSHRuntimeException e) {
            MCBackups.getInstance().getLogger().severe("Invalid SFTP host fingerprint in " + SETTING_PATH + ": use the format SHA256:<base64> or MD5:<aa:bb:...>");
            return null;
        }
    }

    @Override
    public boolean verify(String hostname, int port, PublicKey key) {
        String presented = sha256Fingerprint(key);

        if (expectedFingerprint.isEmpty()) {
            MCBackups.getInstance().getLogger().warning("No SFTP host fingerprint is configured, refusing to connect to " + hostname + ":" + port + ".");
            MCBackups.getInstance().getLogger().warning("The server presented " + presented + ". If this is your server, run /mcbackup sftp trust or set " + SETTING_PATH + " to that value.");
            return false;
        }

        if (delegate != null && delegate.verify(hostname, port, key)) {
            return true;
        }

        MCBackups.getInstance().getLogger().severe("SFTP host key mismatch for " + hostname + ":" + port + ". Expected " + expectedFingerprint + " but the server presented " + presented + ".");
        return false;
    }

    @Override
    public List<String> findExistingAlgorithms(String hostname, int port) {
        return Collections.emptyList();
    }

    public static String sha256Fingerprint(PublicKey key) {
        try {
            byte[] encoded = new Buffer.PlainBuffer().putPublicKey(key).getCompactData();
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(encoded);
            return "SHA256:" + Base64.getEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
