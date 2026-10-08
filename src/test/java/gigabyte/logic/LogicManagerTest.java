package gigabyte.logic;

import static gigabyte.logic.Messages.MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX;
import static gigabyte.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static gigabyte.logic.commands.CommandTestUtil.ADDRESS_DESC_AMY;
import static gigabyte.logic.commands.CommandTestUtil.EMAIL_DESC_AMY;
import static gigabyte.logic.commands.CommandTestUtil.NAME_DESC_AMY;
import static gigabyte.logic.commands.CommandTestUtil.PHONE_DESC_AMY;
import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.AMY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import gigabyte.logic.commands.AddClientCommand;
import gigabyte.logic.commands.CommandResult;
import gigabyte.logic.commands.ListClientsCommand;
import gigabyte.logic.commands.exceptions.CommandException;
import gigabyte.logic.parser.exceptions.ParseException;
import gigabyte.model.GigabyteData;
import gigabyte.model.Model;
import gigabyte.model.ModelManager;
import gigabyte.model.ReadOnlyGigabyteData;
import gigabyte.model.UserPrefs;
import gigabyte.model.client.Client;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.storage.JsonGigabyteDataStorage;
import gigabyte.storage.JsonUserPrefsStorage;
import gigabyte.storage.StorageManager;
import gigabyte.testutil.ClientBuilder;

public class LogicManagerTest {
    private static final IOException DUMMY_IO_EXCEPTION = new IOException("dummy IO exception");
    private static final IOException DUMMY_AD_EXCEPTION = new AccessDeniedException("dummy access denied exception");

    @TempDir
    public Path temporaryFolder;

    private Model model = new ModelManager();
    private Logic logic;

    @BeforeEach
    public void setUp() {
        JsonGigabyteDataStorage gigabyteDataStorage =
                new JsonGigabyteDataStorage(temporaryFolder.resolve("gigabyteData.json"));
        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"));
        StorageManager storage = new StorageManager(gigabyteDataStorage, userPrefsStorage);
        logic = new LogicManager(model, storage);
    }

    @Test
    public void execute_invalidCommandFormat_throwsParseException() {
        String invalidCommand = "uicfhmowqewca";
        assertParseException(invalidCommand, MESSAGE_UNKNOWN_COMMAND);
    }

    @Test
    public void getGigList_returnsModelGigs() {
        assertEquals(model.getGigList(), logic.getGigList());
    }

    @Test
    public void execute_commandExecutionError_throwsCommandException() {
        String deleteCommand = "delete 9";
        assertCommandException(deleteCommand, MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX);
    }

    @Test
    public void execute_validCommand_success() throws Exception {
        String listCommand = ListClientsCommand.COMMAND_WORD;
        assertCommandSuccess(listCommand, ListClientsCommand.MESSAGE_SUCCESS, model);
    }

    @Test
    public void execute_addGig_persistsAcrossReloadAndSubsequentCommands() throws Exception {
        model.addClient(ALICE);
        CommandResult result = logic.execute("addgig 1 s/in_progress d/2027-01-31 f/1250.50");
        Gig expected = new Gig(ALICE, GigStatus.IN_PROGRESS, new Deadline("2027-01-31"), new Fee("1250.50"));
        assertEquals(expected, model.getGigList().get(0));
        assertEquals("New gig added for Alice Pauline: Status: IN_PROGRESS; Deadline: 2027-01-31; "
                + "Agreed fee: 1250.50", result.getFeedbackToUser());

        logic.execute("list");
        JsonGigabyteDataStorage storage = new JsonGigabyteDataStorage(temporaryFolder.resolve("gigabyteData.json"));
        ReadOnlyGigabyteData restored = storage.readGigabyteData().orElseThrow();
        assertEquals(model.getGigabyteData(), new GigabyteData(restored));
        assertSame(restored.getClientList().get(0), restored.getGigList().get(0).getClient());
    }

    @Test
    public void execute_addGigWithInvalidFields_keepsDataUnchanged() {
        model.addClient(ALICE);
        assertParseException("addgig 1 s/NOT_STARTED d/2027-02-29 f/100", Deadline.MESSAGE_CONSTRAINTS);
        assertParseException("addgig 1 s/NOT_STARTED d/2027-01-31 f/0", Fee.MESSAGE_CONSTRAINTS);
        assertCommandException("addgig 2 s/NOT_STARTED d/2027-01-31 f/100", MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX);
    }

    @Test
    public void execute_storageThrowsIoException_throwsCommandException() {
        assertCommandFailureForExceptionFromStorage(DUMMY_IO_EXCEPTION, String.format(
                LogicManager.FILE_OPS_ERROR_FORMAT, DUMMY_IO_EXCEPTION.getMessage()));
    }

    @Test
    public void execute_storageThrowsAdException_throwsCommandException() {
        assertCommandFailureForExceptionFromStorage(DUMMY_AD_EXCEPTION, String.format(
                LogicManager.FILE_OPS_PERMISSION_ERROR_FORMAT, DUMMY_AD_EXCEPTION.getMessage()));
    }

    @Test
    public void getFilteredClientList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> logic.getFilteredClientList().remove(0));
    }

    /**
     * Executes the command and confirms that
     * - no exceptions are thrown <br>
     * - the feedback message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandSuccess(String inputCommand, String expectedMessage,
            Model expectedModel) throws CommandException, ParseException {
        CommandResult result = logic.execute(inputCommand);
        assertEquals(expectedMessage, result.getFeedbackToUser());
        assertEquals(expectedModel, model);
    }

    /**
     * Executes the command, confirms that a ParseException is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertParseException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, ParseException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that a CommandException is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, CommandException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that the exception is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage) {
        Model expectedModel = new ModelManager(model.getGigabyteData(), new UserPrefs());
        assertCommandFailure(inputCommand, expectedException, expectedMessage, expectedModel);
    }

    /**
     * Executes the command and confirms that
     * - the {@code expectedException} is thrown <br>
     * - the resulting error message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>
     * @see #assertCommandSuccess(String, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage, Model expectedModel) {
        assertThrows(expectedException, expectedMessage, () -> logic.execute(inputCommand));
        assertEquals(expectedModel, model);
    }

    /**
     * Tests the Logic component's handling of an {@code IOException} thrown by the Storage component.
     *
     * @param e the exception to be thrown by the Storage component
     * @param expectedMessage the message expected inside exception thrown by the Logic component
     */
    private void assertCommandFailureForExceptionFromStorage(IOException e, String expectedMessage) {
        Path prefPath = temporaryFolder.resolve("ExceptionUserPrefs.json");

        // Inject LogicManager with a JsonGigabyteDataStorage that throws the IOException e when saving
        JsonGigabyteDataStorage gigabyteDataStorage = new JsonGigabyteDataStorage(prefPath) {
            @Override
            public void saveGigabyteData(ReadOnlyGigabyteData gigabyteData) throws IOException {
                throw e;
            }
        };

        JsonUserPrefsStorage userPrefsStorage =
                new JsonUserPrefsStorage(temporaryFolder.resolve("ExceptionUserPrefs.json"));
        StorageManager storage = new StorageManager(gigabyteDataStorage, userPrefsStorage);

        logic = new LogicManager(model, storage);

        // Triggers the saveGigabyteData method by executing an add command
        String addCommand = AddClientCommand.COMMAND_WORD + NAME_DESC_AMY + PHONE_DESC_AMY
                + EMAIL_DESC_AMY + ADDRESS_DESC_AMY;
        Client expectedClient = new ClientBuilder(AMY).withTags().build();
        ModelManager expectedModel = new ModelManager();
        expectedModel.addClient(expectedClient);
        assertCommandFailure(addCommand, CommandException.class, expectedMessage, expectedModel);
    }
}
