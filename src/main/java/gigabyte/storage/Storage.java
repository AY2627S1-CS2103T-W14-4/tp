package gigabyte.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import gigabyte.commons.exceptions.DataLoadingException;
import gigabyte.model.ReadOnlyGigabyteData;
import gigabyte.model.ReadOnlyUserPrefs;
import gigabyte.model.UserPrefs;

/**
 * API of the Storage component
 */
public interface Storage {

    /**
     * Returns the file path of the UserPrefs data file.
     */
    Path getUserPrefsFilePath();

    /**
     * Returns UserPrefs data from storage.
     * Returns {@code Optional.empty()} if storage file is not found.
     *
     * @throws DataLoadingException if the loading of data from preference file failed.
     */
    Optional<UserPrefs> readUserPrefs() throws DataLoadingException;

    /**
     * Saves the given {@link ReadOnlyUserPrefs} to the storage.
     * @param userPrefs cannot be null.
     * @throws IOException if there was any problem writing to the file.
     */
    void saveUserPrefs(ReadOnlyUserPrefs userPrefs) throws IOException;

    /**
     * Returns the file path of the GigabyteData data file.
     */
    Path getGigabyteDataFilePath();

    /**
     * Returns GigabyteData data as a {@link ReadOnlyGigabyteData}.
     * Returns {@code Optional.empty()} if storage file is not found.
     *
     * @throws DataLoadingException if loading the data from storage failed.
     */
    Optional<ReadOnlyGigabyteData> readGigabyteData() throws DataLoadingException;

    /**
     * Saves the given {@link ReadOnlyGigabyteData} to the storage.
     * @param gigabyteData cannot be null.
     * @throws IOException if there was any problem writing to the file.
     */
    void saveGigabyteData(ReadOnlyGigabyteData gigabyteData) throws IOException;

}
