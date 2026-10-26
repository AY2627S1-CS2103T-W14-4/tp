package gigabyte.logic.commands;

import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

public class DeletePaymentCommandTest {
    private static final Index FIRST = Index.fromOneBased(1);
    private static final Index SECOND = Index.fromOneBased(2);
    private static final Index THIRD = Index.fromOneBased(3);

    @Test
    public void constructor_nullIndexes_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new DeletePaymentCommand(null, FIRST, FIRST)); // client
        assertThrows(NullPointerException.class, () -> new DeletePaymentCommand(FIRST, null, FIRST)); // gig
        assertThrows(NullPointerException.class, () -> new DeletePaymentCommand(FIRST, FIRST, null)); // payment
    }

    @Test
    public void execute_nullModel_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new DeletePaymentCommand(FIRST, FIRST, FIRST).execute(null));
    }

    @Test
    public void execute_paidPayment_deletesOnlySelectedPaymentAndReportsPreviousState() throws Exception {
        ModelManager model = createModel();
        Gig gig = addGig(model, ALICE);
        PaymentObligation first = addPayment(model, gig, "25", "2099-01-01", false);
        addPayment(model, gig, "50.25", "2099-01-31", true);
        PaymentObligation last = addPayment(model, gig, "75", "2099-02-01", false);
        GigabyteData before = new GigabyteData(model.getGigabyteData());

        CommandResult result = new DeletePaymentCommand(FIRST, FIRST, SECOND).execute(model);

        assertEquals(List.of(first, last), model.getPaymentObligationList());
        assertSame(first, model.getPaymentObligationList().get(0));
        assertSame(last, model.getPaymentObligationList().get(1));
        assertSame(gig, model.getGigList().get(0));
        assertSame(ALICE, model.getFilteredClientList().get(0));
        assertEquals(before.getClientList(), model.getGigabyteData().getClientList());
        assertEquals(before.getGigList(), model.getGigList());
        assertEquals(new CommandResult("Deleted payment obligation for Alice Pauline, gig 1 (Website redesign), "
                + "payment 2: Amount: 50.25; Due date: 2099-01-31; Previous state: PAID"), result);
    }

    @Test
    public void execute_unpaidFuturePayment_deletesAndReportsUnpaid() throws Exception {
        assertUnpaidDeletion("2099-01-31");
    }

    @Test
    public void execute_overduePayment_deletesAndReportsUnpaid() throws Exception {
        assertUnpaidDeletion("2000-01-31");
    }

    @Test
    public void execute_paymentDisplayOrder_usesDueDateAndRecordedTieOrder() throws Exception {
        ModelManager model = createModel();
        Gig gig = addGig(model, ALICE);
        PaymentObligation latest = addPayment(model, gig, "300", "2099-03-01", false);
        PaymentObligation firstTie = addPayment(model, gig, "100", "2099-01-31", false);
        addPayment(model, gig, "200", "2099-01-31", false);

        new DeletePaymentCommand(FIRST, FIRST, SECOND).execute(model);

        assertEquals(List.of(latest, firstTie), model.getPaymentObligationList());
        assertSame(latest, model.getPaymentObligationList().get(0));
        assertSame(firstTie, model.getPaymentObligationList().get(1));
    }

    @Test
    public void execute_filteredClientList_usesDisplayedClientAndPerClientGigIndexes() throws Exception {
        ModelManager model = createModel();
        Gig aliceGig = addGig(model, ALICE);
        Gig firstBensonGig = addGig(model, BENSON);
        addGig(model, ALICE);
        Gig secondBensonGig = addGig(model, BENSON);
        PaymentObligation alicePayment = addPayment(model, aliceGig, "100", "2099-01-31", false);
        PaymentObligation firstBensonPayment = addPayment(model, firstBensonGig, "100", "2099-01-31", false);
        addPayment(model, secondBensonGig, "100", "2099-01-31", false);
        List<Gig> gigsBefore = List.copyOf(model.getGigList());
        model.updateFilteredClientList(new ClientNameContainsKeywordsPredicate(List.of("Benson")));

        new DeletePaymentCommand(FIRST, SECOND, FIRST).execute(model);

        assertEquals(List.of(alicePayment, firstBensonPayment), model.getPaymentObligationList());
        assertEquals(List.of(BENSON), model.getFilteredClientList());
        assertEquals(gigsBefore, model.getGigList());
    }

    @Test
    public void execute_identicalPayments_deletesOnlySelectedOccurrence() throws Exception {
        ModelManager model = createModel();
        Gig gig = addGig(model, ALICE);
        PaymentObligation first = addPayment(model, gig, "50", "2099-01-31", false);
        addPayment(model, gig, "50", "2099-01-31", false);
        PaymentObligation last = addPayment(model, gig, "75", "2099-02-01", true);

        new DeletePaymentCommand(FIRST, FIRST, SECOND).execute(model);

        assertEquals(2, model.getPaymentObligationList().size());
        assertSame(first, model.getPaymentObligationList().get(0));
        assertSame(last, model.getPaymentObligationList().get(1));

        new DeletePaymentCommand(FIRST, FIRST, FIRST).execute(model);

        assertEquals(1, model.getPaymentObligationList().size());
        assertSame(last, model.getPaymentObligationList().get(0));
    }

    @Test
    public void execute_invalidIndexes_throwsAndLeavesDataUnchanged() {
        ModelManager model = createModel();
        Gig gig = addGig(model, ALICE);
        PaymentObligation payment = addPayment(model, gig, "50", "2099-01-31", false);
        GigabyteData before = new GigabyteData(model.getGigabyteData());

        // EP: client index beyond the displayed client list
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX, () ->
                new DeletePaymentCommand(THIRD, FIRST, FIRST).execute(model));

        // EP: gig index beyond the client's gigs
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_GIG_INDEX, () ->
                new DeletePaymentCommand(FIRST, SECOND, FIRST).execute(model));
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_GIG_INDEX, () ->
                new DeletePaymentCommand(SECOND, FIRST, FIRST).execute(model)); // Client has no gigs

        // EP: payment index beyond the gig's payments
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_PAYMENT_INDEX, () ->
                new DeletePaymentCommand(FIRST, FIRST, SECOND).execute(model));

        assertEquals(before, model.getGigabyteData());
        assertSame(payment, model.getPaymentObligationList().get(0));

        // EP: client index valid in the full list but beyond the filtered list
        model.updateFilteredClientList(new ClientNameContainsKeywordsPredicate(List.of("Alice")));
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX, () ->
                new DeletePaymentCommand(SECOND, FIRST, FIRST).execute(model));
        assertEquals(before, model.getGigabyteData());
        assertEquals(List.of(ALICE), model.getFilteredClientList());
    }

    @Test
    public void execute_emptyPaymentList_throwsAndPreservesGig() {
        ModelManager model = createModel();
        Gig gig = addGig(model, ALICE);
        GigabyteData before = new GigabyteData(model.getGigabyteData());

        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_PAYMENT_INDEX, () ->
                new DeletePaymentCommand(FIRST, FIRST, FIRST).execute(model));

        assertEquals(before, model.getGigabyteData());
        assertSame(gig, model.getGigList().get(0));
    }

    @Test
    public void equals_comparesAllIndexes() {
        DeletePaymentCommand command = new DeletePaymentCommand(FIRST, SECOND, THIRD);
        // same values or same object -> returns true
        assertEquals(command, new DeletePaymentCommand(FIRST, SECOND, THIRD));
        assertTrue(command.equals(command));

        // null or a different type -> returns false
        assertFalse(command.equals(null));
        assertFalse(command.equals("deletepayment"));

        // different client, gig, or payment index -> returns false
        assertFalse(command.equals(new DeletePaymentCommand(SECOND, SECOND, THIRD)));
        assertFalse(command.equals(new DeletePaymentCommand(FIRST, FIRST, THIRD)));
        assertFalse(command.equals(new DeletePaymentCommand(FIRST, SECOND, SECOND)));
    }

    @Test
    public void toStringMethod() {
        DeletePaymentCommand command = new DeletePaymentCommand(FIRST, SECOND, THIRD);
        String expected = DeletePaymentCommand.class.getCanonicalName() + "{clientIndex=" + FIRST
                + ", gigIndex=" + SECOND + ", paymentIndex=" + THIRD + "}";
        assertEquals(expected, command.toString());
    }

    private void assertUnpaidDeletion(String dueDate) throws Exception {
        ModelManager model = createModel();
        Gig gig = addGig(model, ALICE);
        addPayment(model, gig, "50.25", dueDate, false);

        CommandResult result = new DeletePaymentCommand(FIRST, FIRST, FIRST).execute(model);

        assertTrue(model.getPaymentObligationList().isEmpty());
        assertEquals(1, model.getGigList().size());
        assertSame(gig, model.getGigList().get(0));
        assertEquals(List.of(ALICE, BENSON), model.getFilteredClientList());
        assertEquals("Deleted payment obligation for Alice Pauline, gig 1 (Website redesign), "
                + "payment 1: Amount: 50.25; Due date: " + dueDate + "; Previous state: UNPAID",
                result.getFeedbackToUser());
        assertEquals(CommandResult.forOutstandingPayments("0 outstanding payment obligations."),
                new OutstandingCommand().execute(model));
    }

    private ModelManager createModel() {
        ModelManager model = new ModelManager();
        model.addClient(ALICE);
        model.addClient(BENSON);
        return model;
    }

    private Gig addGig(ModelManager model, Client client) {
        model.addGig(new Gig(client, new GigTitle("Website redesign"), GigStatus.NOT_STARTED,
                new Deadline("2099-12-31"), new Fee("1250")));
        return model.getGigList().get(model.getGigList().size() - 1);
    }

    private PaymentObligation addPayment(ModelManager model, Gig gig, String amount, String dueDate,
            boolean isPaid) {
        model.addPaymentObligation(new PaymentObligation(gig, new Fee(amount), new Deadline(dueDate), isPaid));
        return model.getPaymentObligationList().get(model.getPaymentObligationList().size() - 1);
    }
}
