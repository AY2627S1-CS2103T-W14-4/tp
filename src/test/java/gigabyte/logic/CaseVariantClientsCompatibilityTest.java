package gigabyte.logic;

import static gigabyte.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.node.ObjectNode;

import gigabyte.commons.util.JsonUtil;
import gigabyte.logic.commands.EditClientCommand;
import gigabyte.logic.commands.exceptions.CommandException;
import gigabyte.model.Model;
import gigabyte.model.ModelManager;
import gigabyte.model.ReadOnlyGigabyteData;
import gigabyte.model.UserPrefs;
import gigabyte.model.client.Client;
import gigabyte.storage.JsonGigabyteDataStorage;
import gigabyte.storage.JsonUserPrefsStorage;
import gigabyte.storage.StorageManager;

/**
 * Checks that a save file holding clients whose names differ only in case ({@code John Doe} and
 * {@code john doe}) still loads, stays editable, and is saved again without losing data.
 */
public class CaseVariantClientsCompatibilityTest {

    private static final Path FIXTURE_FILE = Paths.get("src", "test", "data",
            "CaseVariantClientsCompatibilityTest", "caseVariantClientsGigabyteData.json");

    @TempDir
    Path temporaryFolder;

    private Path dataFile;
    private StorageManager storage;
    private Model model;
    private Logic logic;

    @BeforeEach
    public void setUp() throws Exception {
        dataFile = temporaryFolder.resolve("gigabyteData.json");
        Files.copy(FIXTURE_FILE, dataFile);
        storage = new StorageManager(new JsonGigabyteDataStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json")));
        model = new ModelManager(storage.readGigabyteData().orElseThrow(), new UserPrefs());
        logic = new LogicManager(model, storage);
    }

    @Test
    public void load_caseVariantPair_allRecordsIntact() {
        assertEquals(List.of("John Doe", "john doe", "Alex Yeoh"), getClientNames(model.getGigabyteData()));
        assertGigsAndPaymentsLinked(model.getGigabyteData());
    }

    @Test
    public void execute_listAfterLoading_savesWithoutLosingData() throws Exception {
        logic.execute("list");

        assertEquals(readJson(FIXTURE_FILE), readJson(dataFile));
    }

    @Test
    public void execute_addNameDifferingOnlyInCase_rejected() {
        assertThrows(CommandException.class, Messages.MESSAGE_DUPLICATE_CLIENT_NAME, () ->
                logic.execute("add n/JOHN DOE p/94444444 e/johnny@example.com a/4 Road"));
    }

    @Test
    public void execute_renameThirdClientToSpacingVariant_rejected() {
        assertThrows(CommandException.class, Messages.MESSAGE_DUPLICATE_CLIENT_NAME, () ->
                logic.execute("edit 3 n/John  Doe"));
    }

    @Test
    public void execute_renameToExactNameOfOtherClient_rejectedByExactCheck() {
        assertThrows(CommandException.class, EditClientCommand.MESSAGE_DUPLICATE_CLIENT, () ->
                logic.execute("edit 1 n/john doe"));
    }

    @Test
    public void execute_editLegacyPair_succeedsAndSavesWithoutLosingData() throws Exception {
        // EP: phone, email, address and tag only edits, for both clients of the pair
        for (int index = 1; index <= 2; index++) {
            logic.execute("edit " + index + " p/8000000" + index);
            logic.execute("edit " + index + " e/new" + index + "@example.com");
            logic.execute("edit " + index + " a/" + index + " New Road");
            logic.execute("edit " + index + " t/vip");
        }

        // EP: capitalisation-only and spacing-only renames of the client's own name
        logic.execute("edit 1 n/JOHN DOE");
        logic.execute("edit 1 n/John  Doe");

        ReadOnlyGigabyteData reloaded = storage.readGigabyteData().orElseThrow();
        assertEquals(List.of("John  Doe", "john doe", "Alex Yeoh"), getClientNames(reloaded));
        assertEquals("80000002", reloaded.getClientList().get(1).getPhone().value);
        assertGigsAndPaymentsLinked(reloaded);
    }

    private static List<String> getClientNames(ReadOnlyGigabyteData data) {
        return data.getClientList().stream().map(client -> client.getName().fullName).toList();
    }

    /**
     * Asserts that each client of the pair still has its own gig, and each gig its own payment.
     */
    private static void assertGigsAndPaymentsLinked(ReadOnlyGigabyteData data) {
        List<Client> clients = data.getClientList();
        assertEquals(2, data.getGigList().size());
        assertEquals(clients.get(0).getUid(), data.getGigList().get(0).getClient().getUid());
        assertEquals(clients.get(1).getUid(), data.getGigList().get(1).getClient().getUid());
        assertEquals(2, data.getPaymentObligationList().size());
        assertEquals(data.getGigList().get(0).getUid(), data.getPaymentObligationList().get(0).getGig().getUid());
        assertEquals(data.getGigList().get(1).getUid(), data.getPaymentObligationList().get(1).getGig().getUid());
    }

    private static ObjectNode readJson(Path path) throws Exception {
        return JsonUtil.fromJsonString(Files.readString(path), ObjectNode.class);
    }
}
