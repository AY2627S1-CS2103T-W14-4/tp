package gigabyte.logic;

import static gigabyte.logic.Messages.MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX;
import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static gigabyte.logic.commands.CommandTestUtil.ADDRESS_DESC_AMY;
import static gigabyte.logic.commands.CommandTestUtil.EMAIL_DESC_AMY;
import static gigabyte.logic.commands.CommandTestUtil.NAME_DESC_AMY;
import static gigabyte.logic.commands.CommandTestUtil.PHONE_DESC_AMY;
import static gigabyte.logic.parser.ParserUtil.MESSAGE_INVALID_INDEX;
import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.AMY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import gigabyte.logic.commands.AddClientCommand;
import gigabyte.logic.commands.CommandResult;
import gigabyte.logic.commands.DeleteGigCommand;
import gigabyte.logic.commands.DeletePaymentCommand;
import gigabyte.logic.commands.EditGigCommand;
import gigabyte.logic.commands.ListClientsCommand;
import gigabyte.logic.commands.OutstandingCommand;
import gigabyte.logic.commands.SetGigStatusCommand;
import gigabyte.logic.commands.exceptions.CommandException;
import gigabyte.logic.parser.CliSyntax;
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
import gigabyte.model.gig.GigTitle;
import gigabyte.model.gig.PaymentObligation;
import gigabyte.model.gig.exceptions.GigHasPaymentsException;
import gigabyte.storage.JsonGigabyteDataStorage;
import gigabyte.storage.JsonUserPrefsStorage;
import gigabyte.storage.StorageManager;
import gigabyte.testutil.ClientBuilder;

public class LogicManagerTest {
    private static final IOException DUMMY_IO_EXCEPTION = new IOException("dummy IO exception");
    private static final IOException DUMMY_AD_EXCEPTION = new AccessDeniedException("dummy access denied exception");

    @TempDir
    Path temporaryFolder;

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
    public void getPaymentObligationList_modelContainsPayments_returnsModelList() {
        assertSame(model.getPaymentObligationList(), logic.getPaymentObligationList());
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
    public void execute_outstanding_preservesRecordsAndRejectsArguments() throws Exception {
        model.addClient(ALICE);
        logic.execute("addgig 1 t/Website s/IN_PROGRESS d/2027-01-31 f/100");
        logic.execute("addpayment 1 1 a/50 d/2027-02-28");
        GigabyteData before = new GigabyteData(model.getGigabyteData());
        Path path = temporaryFolder.resolve("gigabyteData.json");
        String savedData = Files.readString(path);

        assertEquals(CommandResult.forOutstandingPayments("1 outstanding payment obligation."),
                logic.execute("outstanding"));
        assertEquals(before, model.getGigabyteData());
        assertEquals(savedData, Files.readString(path));
        assertParseException("outstanding extra", String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT,
                OutstandingCommand.MESSAGE_USAGE));
        assertEquals(savedData, Files.readString(path));
    }

    @Test
    public void execute_filterGigs_invalidInputPreservesRecordsAndSavedFile() throws Exception {
        model.addClient(ALICE);
        model.addGig(new Gig(ALICE, new GigTitle("Website"), GigStatus.IN_PROGRESS,
                new Deadline("2027-01-31"), new Fee("100")));
        logic.execute("addpayment 1 1 a/50 d/2027-02-28");
        GigabyteData before = new GigabyteData(model.getGigabyteData());
        Path path = temporaryFolder.resolve("gigabyteData.json");
        String savedData = Files.readString(path);

        assertEquals(GigStatus.IN_PROGRESS, logic.execute("gigs s/in_progress").getGigStatusFilter());
        assertEquals(before, model.getGigabyteData());
        assertEquals(savedData, Files.readString(path));
        assertParseException("gigs s/WAITING", GigStatus.MESSAGE_CONSTRAINTS);
        assertEquals(savedData, Files.readString(path));
    }

    @Test
    public void execute_addGig_persistsAcrossReloadAndSubsequentCommands() throws Exception {
        model.addClient(ALICE);
        CommandResult result = logic.execute(
                "addgig 1 t/Website redesign s/in_progress d/2027-01-31 f/1250.50");
        Gig expected = new Gig(ALICE, new GigTitle("Website redesign"), GigStatus.IN_PROGRESS,
                new Deadline("2027-01-31"), new Fee("1250.50"));
        assertEquals(expected, model.getGigList().get(0));
        assertEquals("New gig added for Alice Pauline: Title: Website redesign; Status: IN_PROGRESS; "
                + "Deadline: 2027-01-31; Agreed fee: 1250.50", result.getFeedbackToUser());

        logic.execute("list");
        JsonGigabyteDataStorage storage = new JsonGigabyteDataStorage(temporaryFolder.resolve("gigabyteData.json"));
        ReadOnlyGigabyteData restored = storage.readGigabyteData().orElseThrow();
        assertEquals(model.getGigabyteData(), new GigabyteData(restored));
        assertSame(restored.getClientList().get(0), restored.getGigList().get(0).getClient());
    }

    @Test
    public void execute_addGigWithInvalidFields_keepsDataUnchanged() {
        model.addClient(ALICE);
        assertParseException("addgig 1 t/Website redesign s/NOT_STARTED d/2027-02-29 f/100",
                Deadline.MESSAGE_CONSTRAINTS);
        assertParseException("addgig 1 t/Website redesign s/NOT_STARTED d/2027-01-31 f/0",
                Fee.MESSAGE_CONSTRAINTS);
        assertCommandException("addgig 2 t/Website redesign s/NOT_STARTED d/2027-01-31 f/100",
                MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX);
    }

    @Test
    public void execute_addPayment_persistsAndInvalidInputKeepsRecordsUnchanged() throws Exception {
        model.addClient(ALICE);
        model.addGig(new Gig(ALICE, new GigTitle("Website redesign"), GigStatus.IN_PROGRESS,
                new Deadline("2027-01-31"), new Fee("100")));
        CommandResult result = logic.execute("addpayment 1 1 a/50.25 d/2027-02-28");
        assertEquals("Recorded unpaid obligation for Alice Pauline, gig 1: Amount: 50.25; Due date: 2027-02-28",
                result.getFeedbackToUser());
        GigabyteData before = new GigabyteData(model.getGigabyteData());
        Path path = temporaryFolder.resolve("gigabyteData.json");
        String savedData = Files.readString(path);

        assertParseException("addpayment 1 1 a/0 d/2027-02-28", Fee.MESSAGE_CONSTRAINTS);
        assertParseException("addpayment 1 1 a/50 d/2027-02-29", Deadline.MESSAGE_CONSTRAINTS);
        assertCommandException("addpayment 2 1 a/50 d/2027-02-28", MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX);
        assertCommandException("addpayment 1 2 a/50 d/2027-02-28", Messages.MESSAGE_INVALID_GIG_INDEX);
        assertEquals(before, model.getGigabyteData());
        assertEquals(savedData, Files.readString(path));

        logic.execute("list");
        ReadOnlyGigabyteData restored = new JsonGigabyteDataStorage(path).readGigabyteData().orElseThrow();
        assertEquals(before, new GigabyteData(restored));
        assertSame(restored.getGigList().get(0), restored.getPaymentObligationList().get(0).getGig());
    }

    @Test
    public void execute_markPaid_savesPaidStateForReload() throws Exception {
        model.addClient(ALICE);
        model.addGig(new Gig(ALICE, new GigTitle("Website redesign"), GigStatus.IN_PROGRESS,
                new Deadline("2027-01-31"), new Fee("100")));
        logic.execute("addpayment 1 1 a/50.25 d/2027-02-28");

        CommandResult result = logic.execute("markpaid 1 1 1");

        assertEquals("Marked payment obligation for Alice Pauline, gig 1, payment 1 as paid: Amount: 50.25; "
                + "Due date: 2027-02-28", result.getFeedbackToUser());
        assertTrue(model.getPaymentObligationList().get(0).isPaid());
        ReadOnlyGigabyteData restored = new JsonGigabyteDataStorage(temporaryFolder.resolve("gigabyteData.json"))
                .readGigabyteData().orElseThrow();
        assertTrue(restored.getPaymentObligationList().get(0).isPaid());
        assertEquals(model.getGigabyteData(), new GigabyteData(restored));
    }

    @Test
    public void execute_markUnpaid_savesUnpaidStateForReload() throws Exception {
        model.addClient(ALICE);
        model.addGig(new Gig(ALICE, new GigTitle("Website redesign"), GigStatus.IN_PROGRESS,
                new Deadline("2027-01-31"), new Fee("100")));
        logic.execute("addpayment 1 1 a/50.25 d/2027-02-28");
        logic.execute("markpaid 1 1 1");

        CommandResult result = logic.execute("markunpaid 1 1 1");

        assertEquals("Marked payment obligation for Alice Pauline, gig 1, payment 1 as unpaid: Amount: 50.25; "
                + "Due date: 2027-02-28", result.getFeedbackToUser());
        PaymentObligation payment = model.getPaymentObligationList().get(0);
        assertFalse(payment.isPaid());
        assertEquals(new Fee("50.25"), payment.getAmount());
        assertEquals(new Deadline("2027-02-28"), payment.getDueDate());
        assertSame(model.getGigList().get(0), payment.getGig());
        ReadOnlyGigabyteData restored = new JsonGigabyteDataStorage(temporaryFolder.resolve("gigabyteData.json"))
                .readGigabyteData().orElseThrow();
        PaymentObligation restoredPayment = restored.getPaymentObligationList().get(0);
        assertFalse(restoredPayment.isPaid());
        assertEquals(payment, restoredPayment);
        assertSame(restored.getGigList().get(0), restoredPayment.getGig());
    }

    @Test
    public void execute_deleteGig_persistsExactDeletionAndPreservesOtherPaymentsAcrossRestart() throws Exception {
        model.addClient(ALICE);
        String command = "addgig 1 t/Website s/IN_PROGRESS d/2099-01-31 f/100";
        logic.execute(command);
        logic.execute(command);
        logic.execute("addpayment 1 1 a/50 d/2099-02-28");
        Gig survivor = model.getGigList().get(0);
        Gig removed = model.getGigList().get(1);
        Path path = temporaryFolder.resolve("gigabyteData.json");
        String saved = Files.readString(path);
        for (String invalid : new String[] {"", "1", "1 2 3"}) {
            assertParseException("deletegig " + invalid,
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteGigCommand.MESSAGE_USAGE));
            assertEquals(saved, Files.readString(path));
        }
        for (String invalid : new String[] {"0 1", "1 -1", "1 abc", "2147483648 1"}) {
            assertParseException("deletegig " + invalid, MESSAGE_INVALID_INDEX);
            assertEquals(saved, Files.readString(path));
        }
        assertCommandException("deletegig 2 1", MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX);
        assertCommandException("deletegig 1 3", Messages.MESSAGE_INVALID_GIG_INDEX);
        for (int attempt = 0; attempt < 2; attempt++) {
            assertCommandException("deletegig 1 1", GigHasPaymentsException.MESSAGE);
            assertEquals(saved, Files.readString(path));
            if (attempt == 0) {
                logic.execute("markpaid 1 1 1");
                saved = Files.readString(path);
            }
        }

        logic.execute("deletegig 1 2");

        ReadOnlyGigabyteData restored = new JsonGigabyteDataStorage(path).readGigabyteData().orElseThrow();
        assertEquals(1, restored.getGigList().size());
        assertEquals(survivor.getUid(), restored.getGigList().get(0).getUid());
        assertFalse(restored.getGigList().stream().anyMatch(gig -> gig.hasSameUid(removed)));
        assertEquals(1, restored.getClientList().size());
        assertTrue(restored.getPaymentObligationList().get(0).isPaid());
        assertSame(restored.getGigList().get(0), restored.getPaymentObligationList().get(0).getGig());
        Model restarted = new ModelManager(restored, new UserPrefs());
        assertThrows(GigHasPaymentsException.class, () -> restarted.deleteGig(restarted.getGigList().get(0)));
    }

    @Test
    public void execute_deletePayment_unblocksGigDeletionOnlyAfterAllPaymentsAreRemoved() throws Exception {
        model.addClient(ALICE);
        String command = "addgig 1 t/Website s/IN_PROGRESS d/2099-01-31 f/100";
        logic.execute(command);
        logic.execute(command);
        logic.execute("addpayment 1 1 a/75 d/2099-03-31");
        logic.execute("addpayment 1 2 a/50 d/2099-02-28");
        logic.execute("addpayment 1 2 a/50 d/2099-02-28");
        logic.execute("markpaid 1 2 1");
        Gig survivor = model.getGigList().get(0);
        Gig target = model.getGigList().get(1);
        PaymentObligation otherPayment = model.getPaymentObligationList().get(0);
        assertCommandException("deletegig 1 2", GigHasPaymentsException.MESSAGE);

        CommandResult paidDeletion = logic.execute("deletepayment 1 2 1");
        assertTrue(paidDeletion.getFeedbackToUser().endsWith("Previous state: PAID"));
        assertCommandException("deletegig 1 2", GigHasPaymentsException.MESSAGE);
        CommandResult unpaidDeletion = logic.execute("deletepayment 1 2 1");
        assertTrue(unpaidDeletion.getFeedbackToUser().endsWith("Previous state: UNPAID"));
        assertSame(target, model.getGigList().get(1));

        logic.execute("deletegig 1 2");

        assertEquals(1, model.getGigList().size());
        assertSame(survivor, model.getGigList().get(0));
        assertSame(otherPayment, model.getPaymentObligationList().get(0));
        ReadOnlyGigabyteData restored = new JsonGigabyteDataStorage(temporaryFolder.resolve("gigabyteData.json"))
                .readGigabyteData().orElseThrow();
        assertEquals(1, restored.getGigList().size());
        assertEquals(survivor.getUid(), restored.getGigList().get(0).getUid());
        assertEquals(List.of(otherPayment), restored.getPaymentObligationList());
        assertSame(restored.getGigList().get(0), restored.getPaymentObligationList().get(0).getGig());
        assertEquals(List.of(ALICE), restored.getClientList());
    }

    @Test
    public void execute_deletePayment_savesDeletionAndRemainingOrderAcrossRestart() throws Exception {
        model.addClient(ALICE);
        logic.execute("addgig 1 t/Website s/IN_PROGRESS d/2099-01-31 f/100");
        logic.execute("addpayment 1 1 a/75 d/2099-03-31");
        logic.execute("addpayment 1 1 a/50 d/2099-02-28");
        logic.execute("addpayment 1 1 a/50 d/2099-02-28");
        logic.execute("addpayment 1 1 a/25 d/2000-01-01");
        logic.execute("markpaid 1 1 1");

        logic.execute("deletepayment 1 1 3");
        logic.execute("deletepayment 1 1 1");

        Path path = temporaryFolder.resolve("gigabyteData.json");
        JsonGigabyteDataStorage storage = new JsonGigabyteDataStorage(path);
        Model restarted = new ModelManager(storage.readGigabyteData().orElseThrow(), new UserPrefs());
        assertEquals(model.getGigabyteData(), restarted.getGigabyteData());
        assertEquals(List.of(new Fee("75"), new Fee("50")), restarted.getPaymentObligationList().stream()
                .map(payment -> payment.getAmount()).toList());
        assertSame(restarted.getGigList().get(0), restarted.getPaymentObligationList().get(0).getGig());
        assertSame(restarted.getGigList().get(0), restarted.getPaymentObligationList().get(1).getGig());
        assertEquals(model.getGigList().get(0).getUid(), restarted.getGigList().get(0).getUid());

        Logic restartedLogic = new LogicManager(restarted, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"))));
        restartedLogic.execute("deletepayment 1 1 1");
        restartedLogic.execute("deletepayment 1 1 1");

        ReadOnlyGigabyteData restored = storage.readGigabyteData().orElseThrow();
        assertTrue(restored.getPaymentObligationList().isEmpty());
        assertEquals(restarted.getGigabyteData(), restored);
        assertEquals(model.getGigabyteData().getClientList(), restored.getClientList());
        assertEquals(model.getGigList(), restored.getGigList());
    }

    @Test
    public void execute_deletePayment_invalidInputLeavesDataAndFileUnchanged() throws Exception {
        model.addClient(ALICE);
        logic.execute("addgig 1 t/Website s/IN_PROGRESS d/2099-01-31 f/100");
        logic.execute("addpayment 1 1 a/50 d/2099-02-28");
        GigabyteData before = new GigabyteData(model.getGigabyteData());
        Path path = temporaryFolder.resolve("gigabyteData.json");
        String savedData = Files.readString(path);
        String usage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeletePaymentCommand.MESSAGE_USAGE);

        for (String args : new String[] {"", "1 1", "1 1 1 1"}) {
            assertParseException("deletepayment " + args, usage);
            assertEquals(savedData, Files.readString(path));
        }
        for (String args : new String[] {"0 1 1", "1 -1 1", "1 1 abc"}) {
            assertParseException("deletepayment " + args, MESSAGE_INVALID_INDEX);
            assertEquals(savedData, Files.readString(path));
        }
        assertCommandException("deletepayment 2 1 1", MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX);
        assertCommandException("deletepayment 1 2 1", Messages.MESSAGE_INVALID_GIG_INDEX);
        assertCommandException("deletepayment 1 1 2", Messages.MESSAGE_INVALID_PAYMENT_INDEX);

        assertEquals(before, model.getGigabyteData());
        assertEquals(savedData, Files.readString(path));
    }

    @Test
    public void execute_editPayment_savesEditedDetailsForReload() throws Exception {
        model.addClient(ALICE);
        model.addGig(new Gig(ALICE, new GigTitle("Website redesign"), GigStatus.IN_PROGRESS,
                new Deadline("2027-01-31"), new Fee("100")));
        logic.execute("addpayment 1 1 a/50.25 d/2027-02-28");
        logic.execute("markpaid 1 1 1");

        CommandResult result = logic.execute("editpayment 1 1 1 a/650 d/2027-05-15");

        assertEquals("Edited payment obligation for Alice Pauline, gig 1, payment 1: Amount: 650.00; "
                + "Due date: 2027-05-15", result.getFeedbackToUser());
        assertEquals(new Fee("650"), model.getPaymentObligationList().get(0).getAmount());
        assertEquals(new Deadline("2027-05-15"), model.getPaymentObligationList().get(0).getDueDate());
        assertTrue(model.getPaymentObligationList().get(0).isPaid());
        ReadOnlyGigabyteData restored = new JsonGigabyteDataStorage(temporaryFolder.resolve("gigabyteData.json"))
                .readGigabyteData().orElseThrow();
        assertEquals(model.getGigabyteData(), new GigabyteData(restored));
        assertTrue(restored.getPaymentObligationList().get(0).isPaid());
        assertSame(restored.getGigList().get(0), restored.getPaymentObligationList().get(0).getGig());
    }

    @Test
    public void execute_editGig_rejectsInvalidEditsAtomicallyAndPersistsLinkedPayments() throws Exception {
        model.addClient(ALICE);
        logic.execute("addgig 1 t/Website s/IN_PROGRESS d/2027-01-31 f/100");
        logic.execute("addpayment 1 1 a/50 d/2027-02-28");
        logic.execute("markpaid 1 1 1");
        logic.execute("addpayment 1 1 a/25 d/2027-03-31");
        Gig original = model.getGigList().get(0);
        GigabyteData before = new GigabyteData(model.getGigabyteData());
        Path path = temporaryFolder.resolve("gigabyteData.json");
        String saved = Files.readString(path);

        assertParseException("editgig 1 1 t/Updated d/2027-02-29", Deadline.MESSAGE_CONSTRAINTS);
        assertParseException("editgig 1 1 t/Updated f/0", Fee.MESSAGE_CONSTRAINTS);
        assertParseException("editgig 1 1 t/Updated s/COMPLETED",
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, EditGigCommand.MESSAGE_USAGE));
        assertCommandException("editgig 9 1 t/Updated", MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX);
        assertCommandException("editgig 1 9 t/Updated", Messages.MESSAGE_INVALID_GIG_INDEX);
        assertCommandException("editgig 1 1 t/ Website f/100.00", EditGigCommand.MESSAGE_NOT_EDITED);
        assertEquals(before, model.getGigabyteData());
        assertEquals(saved, Files.readString(path));

        logic.execute("editgig 1 1 t/ Updated website d/2027-04-30 f/1800");
        ReadOnlyGigabyteData restored = new JsonGigabyteDataStorage(path).readGigabyteData().orElseThrow();
        assertEquals(model.getGigabyteData(), new GigabyteData(restored));
        Gig edited = restored.getGigList().get(0);
        assertEquals(original.getUid(), edited.getUid());
        assertEquals(original.getStatus(), edited.getStatus());
        assertEquals(new GigTitle("Updated website"), edited.getTitle());
        assertEquals(new Deadline("2027-04-30"), edited.getDeadline());
        assertEquals(new Fee("1800"), edited.getAgreedFee());
        assertSame(edited, restored.getPaymentObligationList().get(0).getGig());
        assertSame(edited, restored.getPaymentObligationList().get(1).getGig());
        assertTrue(restored.getPaymentObligationList().get(0).isPaid());
        assertFalse(restored.getPaymentObligationList().get(1).isPaid());
    }

    @Test
    public void execute_setGigStatus_usesFilteredClientsAndPersistsStatusAndPaymentLink() throws Exception {
        model.addClient(ALICE);
        model.addClient(AMY);
        model.addGig(new Gig(ALICE, new GigTitle("Website"), GigStatus.NOT_STARTED,
                new Deadline("2027-01-31"), new Fee("100")));
        model.addGig(new Gig(AMY, new GigTitle("Logo"), GigStatus.IN_PROGRESS,
                new Deadline("2027-02-28"), new Fee("200")));
        logic.execute("addpayment 2 1 a/50 d/2027-03-31");
        logic.execute("find Amy");
        Gig targetGig = model.getGigList().get(1);

        CommandResult result = logic.execute("setstatus 1 1 s/cOmPlEtEd");

        assertEquals(GigStatus.COMPLETED, model.getGigList().get(1).getStatus());
        assertEquals(targetGig.getUid(), model.getGigList().get(1).getUid());
        assertSame(model.getGigList().get(1), model.getPaymentObligationList().get(0).getGig());
        assertEquals(String.format(SetGigStatusCommand.MESSAGE_SUCCESS, AMY.getName(), 1, GigStatus.COMPLETED),
                result.getFeedbackToUser());

        Path path = temporaryFolder.resolve("gigabyteData.json");
        ReadOnlyGigabyteData restored = new JsonGigabyteDataStorage(path).readGigabyteData().orElseThrow();
        assertEquals(GigStatus.COMPLETED, restored.getGigList().get(1).getStatus());
        assertEquals(model.getGigabyteData(), new GigabyteData(restored));
        assertSame(restored.getGigList().get(1), restored.getPaymentObligationList().get(0).getGig());
    }

    @Test
    public void execute_setGigStatus_invalidInputLeavesClientsGigsPaymentsAndFileUnchanged() throws Exception {
        model.addClient(ALICE);
        model.addGig(new Gig(ALICE, new GigTitle("Website"), GigStatus.IN_PROGRESS,
                new Deadline("2027-01-31"), new Fee("100")));
        logic.execute("addpayment 1 1 a/50 d/2027-02-28");
        GigabyteData before = new GigabyteData(model.getGigabyteData());
        Path path = temporaryFolder.resolve("gigabyteData.json");
        String savedData = Files.readString(path);
        String usage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, SetGigStatusCommand.MESSAGE_USAGE);

        assertParseException("setstatus 1 1", usage);
        assertParseException("setstatus 1 1 s/WAITING", GigStatus.MESSAGE_CONSTRAINTS);
        assertParseException("setstatus 1 1 s/COMPLETED s/NOT_STARTED",
                Messages.getErrorMessageForDuplicatePrefixes(CliSyntax.PREFIX_STATUS));
        assertParseException("setstatus 1 1 2 s/COMPLETED", usage);
        assertCommandException("setstatus 2 1 s/COMPLETED", MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX);
        assertCommandException("setstatus 1 2 s/COMPLETED", Messages.MESSAGE_INVALID_GIG_INDEX);
        assertCommandException("setstatus 1 1 s/IN_PROGRESS",
                SetGigStatusCommand.MESSAGE_STATUS_UNCHANGED.formatted(GigStatus.IN_PROGRESS));

        assertEquals(before, model.getGigabyteData());
        assertEquals(savedData, Files.readString(path));
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
