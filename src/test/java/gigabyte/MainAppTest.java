package gigabyte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import gigabyte.commons.core.GuiSettings;
import gigabyte.commons.exceptions.DataLoadingException;
import gigabyte.logic.LogicManager;
import gigabyte.logic.commands.exceptions.CommandException;
import gigabyte.model.GigabyteData;
import gigabyte.model.Model;
import gigabyte.model.ModelManager;
import gigabyte.model.ReadOnlyGigabyteData;
import gigabyte.model.UserPrefs;
import gigabyte.model.util.SampleDataUtil;
import gigabyte.storage.JsonGigabyteDataStorage;
import gigabyte.storage.JsonUserPrefsStorage;
import gigabyte.storage.Storage;
import gigabyte.storage.StorageManager;

public class MainAppTest {

    @TempDir
    Path testFolder;

    @Test
    public void initModel_malformedJson_preservesFileBeforeStartingEmpty() throws Exception {
        assertRecoveryAfterLoading("{not valid json");
    }

    @Test
    public void initModel_invalidRecord_preservesFileBeforeStartingEmpty() throws Exception {
        Path fixture = Path.of("src", "test", "data", "JsonGigabyteDataStorageTest",
                "invalidAndValidClientGigabyteData.json");
        assertRecoveryAfterLoading(Files.readString(fixture));
    }

    @Test
    public void initModel_missingFile_startsWithSampleDataWithoutBackupOrWarning() {
        MainApp app = new MainApp();
        Model model = app.initModelManager(createDataStorage(), new UserPrefs());

        assertEquals(SampleDataUtil.getSampleGigabyteData(), model.getGigabyteData());
        assertEquals("", app.startupMessage);
        assertFalse(Files.exists(testFolder.resolve("gigabyte.json_backup-1")));
    }

    @Test
    public void initModel_validFile_loadsWithoutBackupOrWarning() throws Exception {
        Storage storage = createDataStorage();
        ReadOnlyGigabyteData data = SampleDataUtil.getSampleGigabyteData();
        storage.saveGigabyteData(data);
        MainApp app = new MainApp();

        Model model = app.initModelManager(storage, new UserPrefs());

        assertEquals(data, model.getGigabyteData());
        assertEquals("", app.startupMessage);
        assertFalse(Files.exists(testFolder.resolve("gigabyte.json_backup-1")));
    }

    @Test
    public void initModel_backupFailure_blocksChangesAndSavesButAllowsHelpAndExit() throws Exception {
        Storage storage = createDataStorage();
        Path source = storage.getGigabyteDataFilePath();
        Files.createDirectory(source);
        Path existingData = source.resolve("original.txt");
        Files.writeString(existingData, "preserve this");
        MainApp app = new MainApp();

        Model model = app.initModelManager(storage, new UserPrefs());

        assertEquals(new GigabyteData(), model.getGigabyteData());
        assertTrue(app.startupMessage.contains("Backup failed:"));
        assertTrue(app.startupMessage.contains("all commands except help and exit are disabled"));
        assertTrue(storage.isDataSavingBlocked());
        LogicManager logic = new LogicManager(model, storage);
        CommandException error = assertThrows(CommandException.class, () -> logic.execute("list"));
        assertEquals(String.format(LogicManager.DATA_RECOVERY_ERROR_FORMAT, source), error.getMessage());
        assertThrows(CommandException.class, () ->
                logic.execute("add n/Alice p/98765432 e/alice@example.com a/123 Test Street"));
        assertEquals(new GigabyteData(), model.getGigabyteData());
        assertTrue(logic.execute("help").shouldShowHelp());
        assertTrue(logic.execute("exit").shouldExit());
        assertThrows(IOException.class, () -> storage.saveGigabyteData(new GigabyteData()));
        assertEquals("preserve this", Files.readString(existingData));
        assertFalse(Files.exists(testFolder.resolve("gigabyte.json_backup-1")));
    }

    private void assertRecoveryAfterLoading(String contents) throws Exception {
        Storage storage = createDataStorage();
        Path source = storage.getGigabyteDataFilePath();
        Files.writeString(source, contents);
        MainApp app = new MainApp();

        Model model = app.initModelManager(storage, new UserPrefs());

        Path backup = testFolder.resolve("gigabyte.json_backup-1");
        assertEquals(new GigabyteData(), model.getGigabyteData());
        assertEquals(contents, Files.readString(source));
        assertEquals(contents, Files.readString(backup));
        assertTrue(app.startupMessage.contains("corrupted"));
        assertTrue(app.startupMessage.contains("Started with no data."));
        assertTrue(app.startupMessage.contains("Backup saved to " + backup));
        assertFalse(storage.isDataSavingBlocked());

        // Even a read-only command saves, so verify that the backup survives this overwrite.
        new LogicManager(model, storage).execute("list");
        assertEquals(new GigabyteData(), storage.readGigabyteData().orElseThrow());
        assertEquals(contents, Files.readString(backup));
    }

    private Storage createDataStorage() {
        return new StorageManager(new JsonGigabyteDataStorage(testFolder.resolve("gigabyte.json")),
                new JsonUserPrefsStorage(getPrefsPath()));
    }

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
