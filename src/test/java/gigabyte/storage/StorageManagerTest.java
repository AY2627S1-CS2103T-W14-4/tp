package gigabyte.storage;

import static gigabyte.testutil.TypicalClients.getTypicalGigabyteData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import gigabyte.commons.core.GuiSettings;
import gigabyte.model.GigabyteData;
import gigabyte.model.ReadOnlyGigabyteData;
import gigabyte.model.UserPrefs;

public class StorageManagerTest {

    @TempDir
    public Path testFolder;

    private StorageManager storageManager;

    @BeforeEach
    public void setUp() {
        JsonGigabyteDataStorage gigabyteDataStorage = new JsonGigabyteDataStorage(getTempFilePath("ab"));
        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(getTempFilePath("prefs"));
        storageManager = new StorageManager(gigabyteDataStorage, userPrefsStorage);
    }

    private Path getTempFilePath(String fileName) {
        return testFolder.resolve(fileName);
    }

    @Test
    public void prefsReadSave() throws Exception {
        /*
         * Note: This is an integration test that verifies the StorageManager is properly wired to the
         * {@link JsonUserPrefsStorage} class.
         * More extensive testing of UserPref saving/reading is done in {@link JsonUserPrefsStorageTest} class.
         */
        UserPrefs original = new UserPrefs();
        original.setGuiSettings(new GuiSettings(300, 600, 4, 6));
        storageManager.saveUserPrefs(original);
        UserPrefs retrieved = storageManager.readUserPrefs().get();
        assertEquals(original, retrieved);
    }

    @Test
    public void gigabyteDataReadSave() throws Exception {
        /*
         * Note: This is an integration test that verifies the StorageManager is properly wired to the
         * {@link JsonGigabyteDataStorage} class.
         * More extensive testing of UserPref saving/reading is done in {@link JsonGigabyteDataStorageTest} class.
         */
        GigabyteData original = getTypicalGigabyteData();
        storageManager.saveGigabyteData(original);
        ReadOnlyGigabyteData retrieved = storageManager.readGigabyteData().get();
        assertEquals(original, new GigabyteData(retrieved));
    }

    @Test
    public void getGigabyteDataFilePath() {
        assertNotNull(storageManager.getGigabyteDataFilePath());
    }

}
