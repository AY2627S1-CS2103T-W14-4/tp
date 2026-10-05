package gigabyte.storage;

import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.HOON;
import static gigabyte.testutil.TypicalClients.IDA;
import static gigabyte.testutil.TypicalClients.getTypicalGigabyteData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import gigabyte.commons.exceptions.DataLoadingException;
import gigabyte.model.GigabyteData;
import gigabyte.model.ReadOnlyGigabyteData;

public class JsonGigabyteDataStorageTest {
    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonGigabyteDataStorageTest");

    @TempDir
    public Path testFolder;

    @Test
    public void readGigabyteData_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> readGigabyteData(null));
    }

    private java.util.Optional<ReadOnlyGigabyteData> readGigabyteData(String filePath) throws Exception {
        return new JsonGigabyteDataStorage(Paths.get(filePath)).readGigabyteData(addToTestDataPathIfNotNull(filePath));
    }

    private Path addToTestDataPathIfNotNull(String prefsFileInTestDataFolder) {
        return prefsFileInTestDataFolder != null
                ? TEST_DATA_FOLDER.resolve(prefsFileInTestDataFolder)
                : null;
    }

    @Test
    public void read_missingFile_emptyResult() throws Exception {
        assertFalse(readGigabyteData("NonExistentFile.json").isPresent());
    }

    @Test
    public void read_notJsonFormat_exceptionThrown() {
        assertThrows(DataLoadingException.class, () -> readGigabyteData("notJsonFormatGigabyteData.json"));
    }

    @Test
    public void readGigabyteData_invalidClientGigabyteData_throwDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readGigabyteData("invalidClientGigabyteData.json"));
    }

    @Test
    public void readGigabyteData_invalidAndValidClientGigabyteData_throwDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readGigabyteData("invalidAndValidClientGigabyteData.json"));
    }

    @Test
    public void readAndSaveGigabyteData_allInOrder_success() throws Exception {
        Path filePath = testFolder.resolve("TempGigabyteData.json");
        GigabyteData original = getTypicalGigabyteData();
        JsonGigabyteDataStorage jsonGigabyteDataStorage = new JsonGigabyteDataStorage(filePath);

        // Save in new file and read back
        jsonGigabyteDataStorage.saveGigabyteData(original, filePath);
        ReadOnlyGigabyteData readBack = jsonGigabyteDataStorage.readGigabyteData(filePath).get();
        assertEquals(original, new GigabyteData(readBack));

        // Modify data, overwrite existing file, and read back
        original.addClient(HOON);
        original.removeClient(ALICE);
        jsonGigabyteDataStorage.saveGigabyteData(original, filePath);
        readBack = jsonGigabyteDataStorage.readGigabyteData(filePath).get();
        assertEquals(original, new GigabyteData(readBack));

        // Save and read without specifying file path
        original.addClient(IDA);
        jsonGigabyteDataStorage.saveGigabyteData(original); // file path not specified
        readBack = jsonGigabyteDataStorage.readGigabyteData().get(); // file path not specified
        assertEquals(original, new GigabyteData(readBack));

    }

    @Test
    public void saveGigabyteData_nullGigabyteData_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveGigabyteData(null, "SomeFile.json"));
    }

    /**
     * Saves {@code gigabyteData} at the specified {@code filePath}.
     */
    private void saveGigabyteData(ReadOnlyGigabyteData gigabyteData, String filePath) {
        try {
            new JsonGigabyteDataStorage(Paths.get(filePath))
                    .saveGigabyteData(gigabyteData, addToTestDataPathIfNotNull(filePath));
        } catch (IOException ioe) {
            throw new AssertionError("There should not be an error writing to the file.", ioe);
        }
    }

    @Test
    public void saveGigabyteData_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveGigabyteData(new GigabyteData(), null));
    }
}
