package gigabyte.logic.commands;

import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static gigabyte.testutil.TypicalIndexes.INDEX_FIRST_CLIENT;
import static gigabyte.testutil.TypicalIndexes.INDEX_SECOND_CLIENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.Messages;
import gigabyte.logic.commands.exceptions.CommandException;
import gigabyte.model.GigabyteData;
import gigabyte.model.ModelManager;
import gigabyte.model.client.Client;
import gigabyte.model.client.ClientNameContainsKeywordsPredicate;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;
import gigabyte.model.gig.PaymentObligation;

public class AddPaymentCommandTest {
    private static final Index FIRST = INDEX_FIRST_CLIENT;
    private static final Index SECOND = INDEX_SECOND_CLIENT;
    private static final Fee AMOUNT = new Fee("50.25");
    private static final Deadline DUE_DATE = new Deadline("2027-01-31");

    @Test
    public void execute_interleavedClients_linksToIntendedGigAndPreservesData() throws Exception {
        ModelManager model = createModel();
        model.addGig(createGig(ALICE, "100"));
        model.addGig(createGig(BENSON, "200"));
        model.addGig(createGig(ALICE, "300"));
        model.addPaymentObligation(new PaymentObligation(model.getGigList().get(1), new Fee("10"), DUE_DATE, true));
        GigabyteData before = new GigabyteData(model.getGigabyteData());

        CommandResult result = new AddPaymentCommand(FIRST, SECOND, AMOUNT, DUE_DATE).execute(model);

        List<PaymentObligation> obligations = model.getGigabyteData().getPaymentObligationList();
        assertEquals(2, obligations.size());
        assertEquals(before.getPaymentObligationList().get(0), obligations.get(0));
        assertSame(model.getGigList().get(2), obligations.get(1).getGig());
        assertEquals(AMOUNT, obligations.get(1).getAmount());
        assertEquals(DUE_DATE, obligations.get(1).getDueDate());
        assertFalse(obligations.get(1).isPaid());
        assertEquals(before.getClientList(), model.getFilteredClientList());
        assertEquals(before.getGigList(), model.getGigList());
        assertEquals("Recorded unpaid obligation for Alice Pauline, gig 2: Amount: 50.25; Due date: 2027-01-31",
                result.getFeedbackToUser());
    }

    @Test
    public void execute_filteredClientList_usesDisplayedClientIndexAndKeepsFilter() throws Exception {
        ModelManager model = createModel();
        model.addGig(createGig(ALICE, "100"));
        model.addGig(createGig(BENSON, "200"));
        model.updateFilteredClientList(new ClientNameContainsKeywordsPredicate(List.of("Benson")));

        new AddPaymentCommand(FIRST, FIRST, AMOUNT, DUE_DATE).execute(model);

        assertSame(model.getGigList().get(1), model.getGigabyteData().getPaymentObligationList().get(0).getGig());
        assertEquals(List.of(BENSON), model.getFilteredClientList());
    }

    @Test
    public void execute_equalGigs_preservesExactSelectedGig() throws Exception {
        ModelManager model = createModel();
        model.addGig(createGig(ALICE, "100"));
        model.addGig(createGig(ALICE, "100"));

        new AddPaymentCommand(FIRST, SECOND, AMOUNT, DUE_DATE).execute(model);

        assertSame(model.getGigList().get(1), model.getGigabyteData().getPaymentObligationList().get(0).getGig());
        GigabyteData copy = new GigabyteData(model.getGigabyteData());
        assertSame(copy.getGigList().get(1), copy.getPaymentObligationList().get(0).getGig());
    }

    @Test
    public void execute_invalidIndexes_leavesExistingRecordsUnchanged() {
        ModelManager model = createModel();
        model.addGig(createGig(ALICE, "100"));
        model.addPaymentObligation(new PaymentObligation(model.getGigList().get(0), AMOUNT, DUE_DATE, false));
        GigabyteData before = new GigabyteData(model.getGigabyteData());

        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX, () ->
                new AddPaymentCommand(Index.fromOneBased(3), FIRST, AMOUNT, DUE_DATE).execute(model));
        assertThrows(CommandException.class, AddPaymentCommand.MESSAGE_INVALID_GIG_INDEX, () ->
                new AddPaymentCommand(FIRST, SECOND, AMOUNT, DUE_DATE).execute(model));
        assertThrows(CommandException.class, AddPaymentCommand.MESSAGE_INVALID_GIG_INDEX, () ->
                new AddPaymentCommand(SECOND, FIRST, AMOUNT, DUE_DATE).execute(model));
        assertEquals(before, model.getGigabyteData());
        assertEquals(before.getClientList(), model.getFilteredClientList());
    }

    @Test
    public void execute_noClients_throwsCommandException() {
        ModelManager model = new ModelManager();
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX, () ->
                new AddPaymentCommand(FIRST, FIRST, AMOUNT, DUE_DATE).execute(model));
        assertTrue(model.getGigabyteData().getPaymentObligationList().isEmpty());
    }

    @Test
    public void equals_comparesAllFieldsAndHandlesOtherTypes() {
        AddPaymentCommand command = new AddPaymentCommand(FIRST, FIRST, AMOUNT, DUE_DATE);
        assertTrue(command.equals(command));
        assertFalse(command.equals(null));
        assertFalse(command.equals("payment"));
        assertEquals(command, new AddPaymentCommand(FIRST, FIRST, AMOUNT, DUE_DATE));
        assertNotEquals(command, new AddPaymentCommand(SECOND, FIRST, AMOUNT, DUE_DATE));
        assertNotEquals(command, new AddPaymentCommand(FIRST, SECOND, AMOUNT, DUE_DATE));
        assertNotEquals(command, new AddPaymentCommand(FIRST, FIRST, new Fee("75"), DUE_DATE));
        assertNotEquals(command, new AddPaymentCommand(FIRST, FIRST, AMOUNT, new Deadline("2027-02-28")));
    }

    @Test
    public void toStringMethod() {
        AddPaymentCommand command = new AddPaymentCommand(FIRST, SECOND, AMOUNT, DUE_DATE);
        String expected = AddPaymentCommand.class.getCanonicalName() + "{clientIndex=" + FIRST
                + ", gigIndex=" + SECOND + ", amount=" + AMOUNT + ", dueDate=" + DUE_DATE + "}";
        assertEquals(expected, command.toString());
    }

    private ModelManager createModel() {
        ModelManager model = new ModelManager();
        model.addClient(ALICE);
        model.addClient(BENSON);
        return model;
    }

    private Gig createGig(Client client, String fee) {
        return new Gig(client, new GigTitle("Website redesign"), GigStatus.NOT_STARTED, DUE_DATE, new Fee(fee));
    }
}
