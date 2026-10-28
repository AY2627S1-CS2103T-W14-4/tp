package gigabyte;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import gigabyte.commons.core.GuiSettings;
import gigabyte.commons.exceptions.DataLoadingException;
import gigabyte.model.GigabyteData;
import gigabyte.model.ModelManager;
import gigabyte.model.UserPrefs;
import gigabyte.storage.JsonUserPrefsStorage;

public class MainAppTest {

    @TempDir
    Path testFolder;

    @Test
    public void initPrefs_nullGuiSettings_startsWithDefaults() throws IOException, DataLoadingException {
        assertDefaultsAfterLoading("{ \"guiSettings\" : null }");
    }

    @Test
    public void initPrefs_malformedJson_startsWithDefaults() throws IOException, DataLoadingException {
        assertDefaultsAfterLoading("{ \"guiSettings\" : {");
    }

    @Test
    public void initPrefs_wrongGuiSettingType_startsWithDefaults() throws IOException, DataLoadingException {
        assertDefaultsAfterLoading("{ \"guiSettings\" : { \"windowWidth\" : \"abc\" } }");
    }

    @Test
    public void initPrefs_validGuiSettings_restoresWindowSizeAndPosition() throws IOException, DataLoadingException {
        String json = """
                {
                  "guiSettings" : {
                    "windowWidth" : 1200.0,
                    "windowHeight" : 700.0,
                    "windowCoordinates" : { "x" : 30, "y" : 40 }
                  }
                }
                """;
        GuiSettings expected = new GuiSettings(1200, 700, 30, 40);

        assertEquals(expected, loadModelGuiSettings(json));
        assertEquals(expected, readSavedPrefs().getGuiSettings());
    }

    @Test
    public void init_secondInstance_rejectsBeforeWritingAndPreservesFirstInstanceRecords() throws Exception {
        MainApp first = initApp(new MainApp());
        try {
            first.logic.execute("add n/LostRecordTest p/91234567 e/lost@example.com a/Test address");
            Path dataPath = testFolder.resolve("gigabyte.json");
            byte[] savedData = Files.readAllBytes(dataPath);
            // Preserve distinctive formatting to detect even an unnecessary rewrite of preferences.
            Files.writeString(getPrefsPath(), Files.readString(getPrefsPath()) + "\n\n");
            byte[] savedPrefs = Files.readAllBytes(getPrefsPath());

            IOException exception = assertThrows(IOException.class, () -> initApp(new MainApp()));
            assertTrue(exception.getMessage().contains("already running"));
            assertArrayEquals(savedData, Files.readAllBytes(dataPath));
            assertArrayEquals(savedPrefs, Files.readAllBytes(getPrefsPath()));
            first.logic.execute("list");
        } finally {
            first.stop();
        }

        MainApp reopened = initApp(new MainApp());
        try {
            reopened.logic.execute("find LostRecordTest");
            assertEquals(1, reopened.logic.getFilteredClientList().size());
            assertEquals("LostRecordTest", reopened.logic.getFilteredClientList().get(0).getName().fullName);
        } finally {
            reopened.stop();
        }
    }

    @Test
    public void init_preferencesInitializationFails_releasesLock() throws IOException {
        MainApp failing = new MainApp() {
            @Override
            protected UserPrefs initPrefs(JsonUserPrefsStorage storage) {
                throw new IllegalStateException("Preferences initialization failed");
            }
        };
        assertThrows(IllegalStateException.class, () -> initApp(failing));

        MainApp reopened = initApp(new MainApp());
        reopened.stop();
    }

    @Test
    public void stop_preferencesCannotBeSaved_releasesLock() throws IOException {
        MainApp first = initApp(new MainApp());
        Path backup = testFolder.resolve("preferences-backup.json");
        Files.move(getPrefsPath(), backup);
        Files.createDirectory(getPrefsPath());
        try {
            first.stop();
        } finally {
            Files.delete(getPrefsPath());
            Files.move(backup, getPrefsPath());
        }

        MainApp reopened = initApp(new MainApp());
        reopened.stop();
    }

    private MainApp initApp(MainApp app) throws IOException {
        app.init(testFolder.resolve("gigabyte.json"), getPrefsPath(), testFolder.resolve("addressbook.json"));
        return app;
    }

    private void assertDefaultsAfterLoading(String json) throws IOException, DataLoadingException {
        assertEquals(new GuiSettings(), loadModelGuiSettings(json));
        assertEquals(new UserPrefs(), readSavedPrefs());
    }

    private GuiSettings loadModelGuiSettings(String json) throws IOException {
        Files.writeString(getPrefsPath(), json);
        UserPrefs prefs = new MainApp().initPrefs(new JsonUserPrefsStorage(getPrefsPath()));
        return new ModelManager(new GigabyteData(), prefs).getGuiSettings();
    }

    private UserPrefs readSavedPrefs() throws DataLoadingException {
        return new JsonUserPrefsStorage(getPrefsPath()).readUserPrefs().orElseThrow();
    }

    private Path getPrefsPath() {
        return testFolder.resolve("preferences.json");
    }
}
