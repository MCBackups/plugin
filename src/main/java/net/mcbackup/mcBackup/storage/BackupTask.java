package net.mcbackup.mcBackup.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

@AllArgsConstructor
@Getter
@Setter
public abstract class BackupTask {

    private String name;

    public abstract void uploadBackup() throws IOException;

}
