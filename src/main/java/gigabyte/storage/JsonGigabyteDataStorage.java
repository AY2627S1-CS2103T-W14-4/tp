package gigabyte.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import gigabyte.commons.core.LogsCenter;
import gigabyte.commons.exceptions.DataLoadingException;
import gigabyte.commons.exceptions.IllegalValueException;
import gigabyte.commons.util.FileUtil;
import gigabyte.commons.util.JsonUtil;
import gigabyte.model.ReadOnlyGigabyteData;

/**
 * A class to access GigabyteData data stored as a JSON file on the hard disk.
 */
public class JsonGigabyteDataStorage {

    private static final Logger logger = LogsCenter.getLogger(JsonGigabyteDataStorage.class);

    private Path filePath;

    public JsonGigabyteDataStorage(Path filePath) {
        this.filePath = filePath;
    }

    public Path getGigabyteDataFilePath() {
        return filePath;
    }

    /**
     * Returns GigabyteData data as a {@link ReadOnlyGigabyteData}.
     * Returns {@code Optional.empty()} if storage file is not found.
     *
     * @throws DataLoadingException if loading the data from storage failed.
     */
    public Optional<ReadOnlyGigabyteData> readGigabyteData() throws DataLoadingException {
        return readGigabyteData(filePath);
    }

    /**
     * Similar to {@link #readGigabyteData()}.
     *
     * @param filePath location of the data. Cannot be null.
     * @throws DataLoadingException if loading the data from storage failed.
     */
    public Optional<ReadOnlyGigabyteData> readGigabyteData(Path filePath) throws DataLoadingException {
        requireNonNull(filePath);

        Optional<JsonSerializableGigabyteData> jsonGigabyteData = JsonUtil.readJsonFile(
                filePath, JsonSerializableGigabyteData.class);
        if (!jsonGigabyteData.isPresent()) {
            return Optional.empty();
        }

        try {
            return Optional.of(jsonGigabyteData.get().toModelType());
        } catch (IllegalValueException ive) {
            logger.info("Illegal values found in " + filePath + ": " + ive.getMessage());
            throw new DataLoadingException(ive);
        }
    }

    /**
     * Saves the given {@link ReadOnlyGigabyteData} to the storage.
     * @param gigabyteData cannot be null.
     * @throws IOException if there was any problem writing to the file.
     */
    public void saveGigabyteData(ReadOnlyGigabyteData gigabyteData) throws IOException {
        saveGigabyteData(gigabyteData, filePath);
    }

    /**
     * Similar to {@link #saveGigabyteData(ReadOnlyGigabyteData)}.
     *
     * @param filePath location of the data. Cannot be null.
     */
    public void saveGigabyteData(ReadOnlyGigabyteData gigabyteData, Path filePath) throws IOException {
        requireNonNull(gigabyteData);
        requireNonNull(filePath);

        FileUtil.createIfMissing(filePath);
        JsonUtil.saveJsonFile(new JsonSerializableGigabyteData(gigabyteData), filePath);
    }

}
