package gigabyte.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import gigabyte.logic.Logic;
import gigabyte.logic.LogicManager;
import gigabyte.model.ModelManager;

public class DataFileLockTest {
    @TempDir
    Path testFolder;

    @Test
    public void acquire_sameFile_rejectsSecondInstanceAndPreservesSavedClient() throws Exception {
        Path dataFile = testFolder.resolve("gigabyte.json");
        JsonGigabyteDataStorage dataStorage = new JsonGigabyteDataStorage(dataFile);
        try (DataFileLock first = DataFileLock.acquire(dataFile)) {
            Logic logic = new LogicManager(new ModelManager(), new StorageManager(dataStorage,
                    new JsonUserPrefsStorage(testFolder.resolve("preferences.json"))));
            logic.execute("add n/LostRecordTest p/91234567 e/lost@example.com a/Test address");
            String savedData = Files.readString(dataFile);

            assertAlreadyRunning(dataFile);
            assertEquals(savedData, Files.readString(dataFile));
        }
        try (DataFileLock reopened = DataFileLock.acquire(dataFile)) {
            assertEquals("LostRecordTest", dataStorage.readGigabyteData().orElseThrow()
                    .getClientList().getFirst().getName().fullName);
        }
    }

    @Test
    public void acquire_sameJvmRejection_keepsOtherProcessLockedOut() throws Exception {
        Path dataFile = testFolder.resolve("gigabyte.json");
        try (DataFileLock first = DataFileLock.acquire(dataFile)) {
            assertAlreadyRunning(dataFile);
            Process second = startChild(dataFile);
            try {
                assertTrue(second.waitFor(10, TimeUnit.SECONDS), "Second process must reject the locked file");
                assertTrue(second.exitValue() != 0);
                assertTrue(second.inputReader().readAllAsString().contains("already running"));
            } finally {
                stopChild(second);
            }
        }
    }

    @Test
    public void acquire_otherProcess_closingReleasesLock() throws Exception {
        Path dataFile = testFolder.resolve("gigabyte.json");
        Process first = startChild(dataFile);
        try {
            awaitChildLock(first);
            assertAlreadyRunning(dataFile);
            first.getOutputStream().close();
            assertTrue(first.waitFor(10, TimeUnit.SECONDS));
            assertEquals(0, first.exitValue());
            try (DataFileLock reopened = DataFileLock.acquire(dataFile)) {
                assertFalse(Files.exists(dataFile));
            }
        } finally {
            stopChild(first);
        }
    }

    @Test
    public void acquire_otherProcessCrash_releasesLock() throws Exception {
        Path dataFile = testFolder.resolve("gigabyte.json");
        Process first = startChild(dataFile);
        try {
            awaitChildLock(first);
            assertAlreadyRunning(dataFile);
            stopChild(first);
            try (DataFileLock reopened = DataFileLock.acquire(dataFile)) {
                assertFalse(Files.exists(dataFile));
            }
        } finally {
            stopChild(first);
        }
    }

    @Test
    public void close_persistentSidecar_allowsReopeningWithoutDeletingLockFile() throws Exception {
        Path dataFile = testFolder.resolve("gigabyte.json");
        List<Path> sidecars;
        try (DataFileLock first = DataFileLock.acquire(dataFile);
                Stream<Path> paths = Files.list(testFolder)) {
            sidecars = paths.toList();
            assertEquals(1, sidecars.size());
            assertFalse(Files.exists(dataFile));
        }
        assertTrue(Files.exists(sidecars.getFirst()));
        try (DataFileLock reopened = DataFileLock.acquire(dataFile)) {
            assertTrue(Files.exists(sidecars.getFirst()));
        }
    }

    @Test
    public void acquire_differentFilesAndMissingDirectory_allowsIndependentInstances() throws Exception {
        Path firstFile = testFolder.resolve("nested/data/gigabyte.json");
        Path secondFile = testFolder.resolve("nested/data/other.json");
        try (DataFileLock first = DataFileLock.acquire(firstFile);
                DataFileLock second = DataFileLock.acquire(secondFile)) {
            assertTrue(Files.isDirectory(firstFile.getParent()));
            assertFalse(Files.exists(firstFile));
            assertFalse(Files.exists(secondFile));
        }
    }

    @Test
    public void acquire_symbolicFileAndParentAliases_rejectsSameDataFile() throws Exception {
        Path directory = Files.createDirectory(testFolder.resolve("actual"));
        Path dataFile = Files.writeString(directory.resolve("gigabyte.json"), "{}");
        Path fileAlias = testFolder.resolve("alias.json");
        Path directoryAlias = testFolder.resolve("directory-alias");
        try {
            Files.createSymbolicLink(fileAlias, dataFile);
            Files.createSymbolicLink(directoryAlias, directory);
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            assumeTrue(false, "Symbolic links unavailable: " + e.getMessage());
        }
        try (DataFileLock first = DataFileLock.acquire(dataFile)) {
            assertAlreadyRunning(fileAlias);
            assertAlreadyRunning(directoryAlias.resolve("gigabyte.json"));
        }
    }

    private void assertAlreadyRunning(Path dataFile) {
        IOException exception = assertThrows(IOException.class, () -> DataFileLock.acquire(dataFile));
        assertTrue(exception.getMessage().contains("already running"));
    }

    private Process startChild(Path dataFile) throws Exception {
        String classPath = Path.of(DataFileLockTest.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                + File.pathSeparator
                + Path.of(DataFileLock.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        return new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", classPath, LockHolder.class.getName(), dataFile.toString()).redirectErrorStream(true).start();
    }

    private void awaitChildLock(Process child) throws Exception {
        String output = CompletableFuture.supplyAsync(() -> child.inputReader().lines()
                .filter("LOCKED"::equals).findFirst().orElse(null)).get(10, TimeUnit.SECONDS);
        assertEquals("LOCKED", output);
    }

    private void stopChild(Process child) throws InterruptedException {
        if (child.isAlive()) {
            child.destroyForcibly();
            assertTrue(child.waitFor(10, TimeUnit.SECONDS));
        }
    }

    /**
     * Holds a lock in an independent JVM until its standard input is closed.
     */
    public static class LockHolder {
        public static void main(String[] args) throws Exception {
            try (DataFileLock lock = DataFileLock.acquire(Path.of(args[0]))) {
                System.out.println("LOCKED");
                System.out.flush();
                System.in.read();
            }
        }
    }
}
