package gigabyte.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import gigabyte.commons.core.LogsCenter;
import gigabyte.commons.exceptions.DataLoadingException;
import gigabyte.model.ReadOnlyGigabyteData;
import gigabyte.model.ReadOnlyUserPrefs;
import gigabyte.model.UserPrefs;

/**
 * Manages storage of GigabyteData data in local storage.
 */
public class StorageManager implements Storage {

    private static final Logger logger = LogsCenter.getLogger(StorageManager.class);
    private JsonGigabyteDataStorage gigabyteDataStorage;
    private JsonUserPrefsStorage userPrefsStorage;

    /**
     * Creates a {@code StorageManager} with the given Gigabyte data and user prefs storage.
     */
    public StorageManager(JsonGigabyteDataStorage gigabyteDataStorage, JsonUserPrefsStorage userPrefsStorage) {
        this.gigabyteDataStorage = gigabyteDataStorage;
        this.userPrefsStorage = userPrefsStorage;
    }

    // ================ UserPrefs methods ==============================

    @Override
    public Path getUserPrefsFilePath() {
        return userPrefsStorage.getUserPrefsFilePath();
    }

    @Override
    public Optional<UserPrefs> readUserPrefs() throws DataLoadingException {
        return userPrefsStorage.readUserPrefs();
    }

    @Override
    public void saveUserPrefs(ReadOnlyUserPrefs userPrefs) throws IOException {
        userPrefsStorage.saveUserPrefs(userPrefs);
    }


    // ================ GigabyteData methods ==============================

    @Override
    public Path getGigabyteDataFilePath() {
        return gigabyteDataStorage.getGigabyteDataFilePath();
    }

    @Override
    public Optional<ReadOnlyGigabyteData> readGigabyteData() throws DataLoadingException {
        logger.fine("Attempting to read data from file: " + gigabyteDataStorage.getGigabyteDataFilePath());
        return gigabyteDataStorage.readGigabyteData();
    }

    @Override
    public void saveGigabyteData(ReadOnlyGigabyteData gigabyteData) throws IOException {
        logger.fine("Attempting to write to data file: " + gigabyteDataStorage.getGigabyteDataFilePath());
        gigabyteDataStorage.saveGigabyteData(gigabyteData);
    }

}
