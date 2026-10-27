package gigabyte;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
