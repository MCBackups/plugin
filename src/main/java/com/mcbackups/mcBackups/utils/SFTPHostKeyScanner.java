package com.mcbackups.mcBackups.utils;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class SFTPHostKeyScanner {

    public static String fetchFingerprint(String host, int port) throws IOException {
        AtomicReference<String> fingerprint = new AtomicReference<>();

        try (SSHClient ssh = new SSHClient()) {
            ssh.setConnectTimeout(15_000);
            ssh.setTimeout(15_000);

            ssh.addHostKeyVerifier(new HostKeyVerifier() {
                @Override
                public boolean verify(String hostname, int port, PublicKey key) {
                    fingerprint.set(SFTPHostKeyVerifier.sha256Fingerprint(key));
                    return false;
                }

                @Override
                public List<String> findExistingAlgorithms(String hostname, int port) {
                    return Collections.emptyList();
                }
            });

            try {
                ssh.connect(host, port);
            } catch (TransportException e) {
                if (fingerprint.get() == null) {
                    throw e;
                }
            }
        }

        if (fingerprint.get() == null) {
            throw new IOException("The server did not present a host key.");
        }

        return fingerprint.get();
    }

}
