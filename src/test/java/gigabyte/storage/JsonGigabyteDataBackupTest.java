package gigabyte.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import gigabyte.model.GigabyteData;

public class JsonGigabyteDataBackupTest {
    @TempDir
    Path testFolder;

    @Test
    public void backup_corruptedBytes_preservesOriginalAndExactContents() throws Exception {
        Path source = testFolder.resolve("gigabyte.json");
        byte[] corrupted = new byte[] {'{', 0, (byte) 0xff, '\r', '\n'};
        Files.write(source, corrupted);
        JsonGigabyteDataStorage storage = new JsonGigabyteDataStorage(source);

        Path backup = storage.backupGigabyteData();

        assertEquals(testFolder.resolve("gigabyte.json_backup-1"), backup);
        assertArrayEquals(corrupted, Files.readAllBytes(source));
        assertArrayEquals(corrupted, Files.readAllBytes(backup));
        assertFalse(storage.isDataSavingBlocked());
    }

    @Test
    public void backup_existingNames_skipsFilesAndDirectoriesWithoutReplacingThem() throws Exception {
        Path source = testFolder.resolve("gigabyte.json");
        Files.writeString(source, "{corrupted");
        Path first = testFolder.resolve("gigabyte.json_backup-1");
        Files.writeString(first, "older backup");
        Path second = testFolder.resolve("gigabyte.json_backup-2");
        Files.createDirectory(second);
        Path fourth = testFolder.resolve("gigabyte.json_backup-4");
        Files.writeString(fourth, "another backup");
        JsonGigabyteDataStorage storage = new JsonGigabyteDataStorage(source);

        assertEquals(testFolder.resolve("gigabyte.json_backup-3"), storage.backupGigabyteData());
        assertEquals(testFolder.resolve("gigabyte.json_backup-5"), storage.backupGigabyteData());
        assertEquals("older backup", Files.readString(first));
        assertTrue(Files.isDirectory(second));
        assertEquals("another backup", Files.readString(fourth));
        assertEquals("{corrupted", Files.readString(testFolder.resolve("gigabyte.json_backup-3")));
        assertEquals("{corrupted", Files.readString(testFolder.resolve("gigabyte.json_backup-5")));
        assertEquals("{corrupted", Files.readString(source));
    }

    @Test
    public void backup_existingBackupLinksToOriginal_createsIndependentCopy() throws Exception {
        Path source = testFolder.resolve("gigabyte.json");
        Files.writeString(source, "{corrupted");
        Path existingLink = testFolder.resolve("gigabyte.json_backup-1");
        Files.createLink(existingLink, source);
        JsonGigabyteDataStorage storage = new JsonGigabyteDataStorage(source);

        Path backup = storage.backupGigabyteData();

        assertEquals(testFolder.resolve("gigabyte.json_backup-2"), backup);
        assertTrue(Files.isSameFile(source, existingLink));
        storage.saveGigabyteData(new GigabyteData());
        assertEquals("{corrupted", Files.readString(backup));
    }

    @Test
    public void backup_failure_blocksWritesUntilBackupSucceeds() throws Exception {
        Path source = testFolder.resolve("gigabyte.json");
        JsonGigabyteDataStorage storage = new JsonGigabyteDataStorage(source);
        assertThrows(IOException.class, storage::backupGigabyteData);
        assertTrue(storage.isDataSavingBlocked());

        Files.writeString(source, "{corrupted");
        assertThrows(IOException.class, () -> storage.saveGigabyteData(new GigabyteData()));
        assertEquals("{corrupted", Files.readString(source));

        Path backup = storage.backupGigabyteData();
        assertFalse(storage.isDataSavingBlocked());
        storage.saveGigabyteData(new GigabyteData());
        assertEquals(new GigabyteData(), storage.readGigabyteData().orElseThrow());
        assertEquals("{corrupted", Files.readString(backup));
    }

    @Test
    public void backup_sourceIsDirectory_failsWithoutCreatingMisleadingBackup() throws Exception {
        Path source = Files.createDirectory(testFolder.resolve("gigabyte.json"));
        JsonGigabyteDataStorage storage = new JsonGigabyteDataStorage(source);

        assertThrows(IOException.class, storage::backupGigabyteData);
        assertTrue(storage.isDataSavingBlocked());
        assertTrue(Files.isDirectory(source));
        assertFalse(Files.exists(testFolder.resolve("gigabyte.json_backup-1")));
    }
}
