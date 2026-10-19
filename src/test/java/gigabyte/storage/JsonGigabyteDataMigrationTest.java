package gigabyte.storage;

import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.HOON;
import static gigabyte.testutil.TypicalClients.getTypicalGigabyteData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import gigabyte.commons.exceptions.DataLoadingException;
import gigabyte.model.GigabyteData;
import gigabyte.model.ReadOnlyGigabyteData;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;
import gigabyte.model.gig.PaymentObligation;

/** Tests migration from the previous save-file name without changing its contents. */
public class JsonGigabyteDataMigrationTest {
    @TempDir
    public Path testFolder;

    private Path legacyFilePath;
    private Path currentFilePath;
    private JsonGigabyteDataStorage storage;

    @BeforeEach
    public void setUp() {
        legacyFilePath = testFolder.resolve("addressbook.json");
        currentFilePath = testFolder.resolve("gigabyte.json");
        storage = new JsonGigabyteDataStorage(currentFilePath);
    }

    @Test
    public void migrateLegacyDataFile_legacyFileOnly_preservesDataAndSavesToCurrentFile() throws Exception {
        GigabyteData original = getTypicalGigabyteData();
        original.addGig(new Gig(ALICE, new GigTitle("Website redesign"), GigStatus.IN_PROGRESS,
                new Deadline("2027-01-31"), new Fee("1250.50")));
        original.addPaymentObligation(new PaymentObligation(original.getGigList().get(0), new Fee("50.25"),
                new Deadline("2027-02-28"), true));
        storage.saveGigabyteData(original, legacyFilePath);
        String originalContents = Files.readString(legacyFilePath);

        storage.migrateLegacyDataFile(legacyFilePath);

        assertFalse(Files.exists(legacyFilePath));
        assertEquals(originalContents, Files.readString(currentFilePath));
        ReadOnlyGigabyteData restored = storage.readGigabyteData().orElseThrow();
        assertEquals(original, new GigabyteData(restored));
        assertSame(restored.getClientList().get(0), restored.getGigList().get(0).getClient());
        assertSame(restored.getGigList().get(0), restored.getPaymentObligationList().get(0).getGig());

        original.addClient(HOON);
        storage.saveGigabyteData(original);
        storage.migrateLegacyDataFile(legacyFilePath);
        assertEquals(original, new GigabyteData(storage.readGigabyteData().orElseThrow()));
        assertFalse(Files.exists(legacyFilePath));
    }

    @Test
    public void migrateLegacyDataFile_bothFilesExist_preservesBothAndLoadsCurrentFile() throws Exception {
        GigabyteData legacyData = getTypicalGigabyteData();
        storage.saveGigabyteData(legacyData, legacyFilePath);
        storage.saveGigabyteData(new GigabyteData());
        String legacyContents = Files.readString(legacyFilePath);
        String currentContents = Files.readString(currentFilePath);

        storage.migrateLegacyDataFile(legacyFilePath);

        assertEquals(legacyContents, Files.readString(legacyFilePath));
        assertEquals(currentContents, Files.readString(currentFilePath));
        assertEquals(new GigabyteData(), storage.readGigabyteData().orElseThrow());
    }

    @Test
    public void migrateLegacyDataFile_invalidCurrentFile_doesNotReplaceWithLegacyFile() throws Exception {
        storage.saveGigabyteData(getTypicalGigabyteData(), legacyFilePath);
        String legacyContents = Files.readString(legacyFilePath);
        Files.writeString(currentFilePath, "{invalid json");

        storage.migrateLegacyDataFile(legacyFilePath);

        assertEquals(legacyContents, Files.readString(legacyFilePath));
        assertEquals("{invalid json", Files.readString(currentFilePath));
        assertThrows(DataLoadingException.class, storage::readGigabyteData);
    }

    @Test
    public void migrateLegacyDataFile_currentFileOnly_preservesCurrentFile() throws Exception {
        storage.saveGigabyteData(getTypicalGigabyteData());
        String currentContents = Files.readString(currentFilePath);

        storage.migrateLegacyDataFile(legacyFilePath);

        assertEquals(currentContents, Files.readString(currentFilePath));
        assertFalse(Files.exists(legacyFilePath));
    }

    @Test
    public void migrateLegacyDataFile_neitherFileExists_keepsMissingDataResult() throws Exception {
        storage.migrateLegacyDataFile(legacyFilePath);

        assertFalse(Files.exists(currentFilePath));
        assertFalse(Files.exists(legacyFilePath));
        assertFalse(storage.readGigabyteData().isPresent());
    }

    @Test
    public void migrateLegacyDataFile_moveFails_throwsExceptionAndPreservesLegacyFile() throws Exception {
        storage.saveGigabyteData(getTypicalGigabyteData(), legacyFilePath);
        String legacyContents = Files.readString(legacyFilePath);
        Path unavailableTarget = testFolder.resolve("missing-directory").resolve("gigabyte.json");
        JsonGigabyteDataStorage unavailableStorage = new JsonGigabyteDataStorage(unavailableTarget);

        assertThrows(IOException.class, () -> unavailableStorage.migrateLegacyDataFile(legacyFilePath));

        assertEquals(legacyContents, Files.readString(legacyFilePath));
        assertFalse(Files.exists(unavailableTarget));
    }
}
