package gigabyte.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

    private boolean dataSavingBlocked;
    private final Path filePath;

    public JsonGigabyteDataStorage(Path filePath) {
        this.filePath = filePath;
    }

    public Path getGigabyteDataFilePath() {
        return filePath;
    }

    /**
     * Returns whether an unsuccessful backup is preventing data saves.
     */
    public boolean isDataSavingBlocked() {
        return dataSavingBlocked;
    }

    /**
     * Copies the unreadable savefile to the first available {@code <filename>_backup-N} beside it.
     * Existing backups are never replaced. Saves are blocked if a complete backup cannot be created.
     *
     * @return the path of the completed backup.
     * @throws IOException if the file cannot be backed up.
     */
    public Path backupGigabyteData() throws IOException {
        dataSavingBlocked = true;
        if (!Files.isRegularFile(filePath)) {
            throw new IOException("The data file is missing or is not a regular file: " + filePath);
        }

        for (long number = 1; number > 0; number++) {
            Path backupPath = filePath.resolveSibling(filePath.getFileName() + "_backup-" + number);
            // Windows may report an existing directory as access denied rather than file already exists.
            if (Files.exists(backupPath, LinkOption.NOFOLLOW_LINKS)) {
                continue;
            }
            // CREATE_NEW reserves the name without following or replacing an existing file or link.
            try (OutputStream backup = Files.newOutputStream(backupPath, StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE)) {
                Files.copy(filePath, backup);
            } catch (FileAlreadyExistsException e) {
                continue;
            }
            dataSavingBlocked = false;
            logger.info("Backed up unreadable data file to " + backupPath);
            return backupPath;
        }
        throw new IOException("No available backup filename for " + filePath);
    }

    /**
     * Moves the legacy save file to this storage's path if the current save file does not exist.
     * Preserves the file contents without changing the save format. An existing current file takes precedence.
     *
     * @throws IOException if the legacy file cannot be moved. Callers should stop initialization on failure.
     */
    public void migrateLegacyDataFile(Path legacyFilePath) throws IOException {
        requireNonNull(legacyFilePath);
        if (Files.exists(filePath) || !Files.exists(legacyFilePath)) {
            return;
        }

        // Do not replace a current save file, even if it was created after the existence check.
        Files.move(legacyFilePath, filePath);
        logger.info("Migrated data file from " + legacyFilePath + " to " + filePath);
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

        Optional<JsonSerializableGigabyteData> jsonGigabyteData = JsonUtil.readJsonFileStrict(
                filePath, JsonSerializableGigabyteData.class);
        if (jsonGigabyteData.isEmpty()) {
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

        if (dataSavingBlocked) {
            throw new IOException("Saving is disabled because the unreadable data file could not be backed up. "
                    + "Back up or repair " + this.filePath + " and restart Gigabyte.");
        }

        FileUtil.createIfMissing(filePath);
        JsonUtil.saveJsonFile(new JsonSerializableGigabyteData(gigabyteData), filePath);
    }

}
