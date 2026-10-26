package gigabyte.logic.commands;

import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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

public class MarkPaymentUnpaidCommandTest {
    private static final Index FIRST = Index.fromOneBased(1);
    private static final Index SECOND = Index.fromOneBased(2);
    private static final Index THIRD = Index.fromOneBased(3);
    private static final Fee AMOUNT = new Fee("50.25");
    private static final Deadline DUE_DATE = new Deadline("2027-01-31");

    @Test
    public void execute_paidPayment_marksOnlySelectedUnpaidAndPreservesDetails() throws Exception {
        ModelManager model = createModel();
        Gig gig = addGig(model, ALICE);
        model.addPaymentObligation(new PaymentObligation(gig, new Fee("25"),
                new Deadline("2027-01-01"), true));
        model.addPaymentObligation(new PaymentObligation(gig, AMOUNT, DUE_DATE, true));
        PaymentObligation selectedPayment = model.getPaymentObligationList().get(1);
        PaymentObligation firstPayment = model.getPaymentObligationList().get(0);
        GigabyteData before = new GigabyteData(model.getGigabyteData());

        CommandResult result = new MarkPaymentUnpaidCommand(FIRST, FIRST, SECOND).execute(model);

        PaymentObligation updatedPayment = model.getPaymentObligationList().get(1);
        assertFalse(updatedPayment.isPaid());
        assertNotSame(selectedPayment, updatedPayment);
        assertTrue(model.getPaymentObligationList().get(0).isPaid());
        assertEquals(AMOUNT, updatedPayment.getAmount());
        assertEquals(DUE_DATE, updatedPayment.getDueDate());
        assertSame(selectedPayment.getGig(), updatedPayment.getGig());
        assertEquals(firstPayment, model.getPaymentObligationList().get(0));
        assertEquals(before.getClientList(), model.getGigabyteData().getClientList());
        assertEquals(before.getGigList(), model.getGigabyteData().getGigList());
        assertEquals("Marked payment obligation for Alice Pauline, gig 1, payment 2 as unpaid: Amount: 50.25; "
                + "Due date: 2027-01-31", result.getFeedbackToUser());
    }

    @Test
    public void execute_paymentDisplayOrder_usesDueDateAndRecordedTieOrder() throws Exception {
        ModelManager model = createModel();
        Gig gig = addGig(model, ALICE);
        model.addPaymentObligation(new PaymentObligation(gig, new Fee("300"),
                new Deadline("2027-03-01"), true));
        model.addPaymentObligation(new PaymentObligation(gig, new Fee("100"), DUE_DATE, true));
        model.addPaymentObligation(new PaymentObligation(gig, new Fee("200"), DUE_DATE, true));

        new MarkPaymentUnpaidCommand(FIRST, FIRST, SECOND).execute(model);

        assertTrue(model.getPaymentObligationList().get(0).isPaid());
        assertTrue(model.getPaymentObligationList().get(1).isPaid());
        assertFalse(model.getPaymentObligationList().get(2).isPaid());
    }

    @Test
    public void execute_filteredClientList_usesDisplayedClientIndex() throws Exception {
        ModelManager model = createModel();
        Gig aliceGig = addGig(model, ALICE);
        Gig bensonGig = addGig(model, BENSON);
        model.addPaymentObligation(new PaymentObligation(aliceGig, AMOUNT, DUE_DATE, true));
        model.addPaymentObligation(new PaymentObligation(bensonGig, AMOUNT, DUE_DATE, true));
        model.updateFilteredClientList(new ClientNameContainsKeywordsPredicate(List.of("Benson")));

        new MarkPaymentUnpaidCommand(FIRST, FIRST, FIRST).execute(model);

        assertTrue(model.getPaymentObligationList().get(0).isPaid());
        assertFalse(model.getPaymentObligationList().get(1).isPaid());
        assertEquals(List.of(BENSON), model.getFilteredClientList());
    }

    @Test
    public void execute_duplicatePayments_marksOnlySelectedOccurrence() throws Exception {
        ModelManager model = createModel();
        Gig gig = addGig(model, ALICE);
        model.addPaymentObligation(new PaymentObligation(gig, AMOUNT, DUE_DATE, true));
        model.addPaymentObligation(new PaymentObligation(gig, AMOUNT, DUE_DATE, true));
        PaymentObligation selectedSecond = model.getPaymentObligationList().get(1);
        PaymentObligation first = model.getPaymentObligationList().get(0);

        new MarkPaymentUnpaidCommand(FIRST, FIRST, SECOND).execute(model);

        assertTrue(model.getPaymentObligationList().get(0).isPaid());
        assertFalse(model.getPaymentObligationList().get(1).isPaid());
        assertSame(first, model.getPaymentObligationList().get(0));
        assertNotSame(selectedSecond, model.getPaymentObligationList().get(1));
        assertSame(selectedSecond.getGig(), model.getPaymentObligationList().get(1).getGig());
    }

    @Test
    public void execute_alreadyUnpaid_throwsAndLeavesDataUnchanged() {
        ModelManager model = createModel();
        Gig gig = addGig(model, ALICE);
        model.addPaymentObligation(new PaymentObligation(gig, AMOUNT, DUE_DATE, false));
        GigabyteData before = new GigabyteData(model.getGigabyteData());
        PaymentObligation alreadyUnpaid = model.getPaymentObligationList().get(0);

        assertThrows(CommandException.class, MarkPaymentUnpaidCommand.MESSAGE_ALREADY_UNPAID, () ->
                new MarkPaymentUnpaidCommand(FIRST, FIRST, FIRST).execute(model));

        assertEquals(before, model.getGigabyteData());
        assertSame(alreadyUnpaid, model.getPaymentObligationList().get(0));
    }

    @Test
    public void execute_invalidIndexes_throwsAndLeavesDataUnchanged() {
        ModelManager model = createModel();
        Gig gig = addGig(model, ALICE);
        model.addPaymentObligation(new PaymentObligation(gig, AMOUNT, DUE_DATE, true));
        GigabyteData before = new GigabyteData(model.getGigabyteData());

        // EP: client index beyond the displayed client list
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX, () ->
                new MarkPaymentUnpaidCommand(THIRD, FIRST, FIRST).execute(model));

        // EP: gig index beyond the client's gigs
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_GIG_INDEX, () ->
                new MarkPaymentUnpaidCommand(FIRST, SECOND, FIRST).execute(model));

        // EP: payment index beyond the gig's payments
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_PAYMENT_INDEX, () ->
                new MarkPaymentUnpaidCommand(FIRST, FIRST, SECOND).execute(model));

        assertEquals(before, model.getGigabyteData());
    }

    @Test
    public void equals_comparesAllIndexes() {
        MarkPaymentUnpaidCommand command = new MarkPaymentUnpaidCommand(FIRST, SECOND, THIRD);
        // same values or same object -> returns true
        assertEquals(command, new MarkPaymentUnpaidCommand(FIRST, SECOND, THIRD));
        assertTrue(command.equals(command));

        // null or a different type -> returns false
        assertFalse(command.equals(null));
        assertFalse(command.equals("markunpaid"));

        // different client, gig, or payment index -> returns false
        assertFalse(command.equals(new MarkPaymentUnpaidCommand(SECOND, SECOND, THIRD)));
        assertFalse(command.equals(new MarkPaymentUnpaidCommand(FIRST, FIRST, THIRD)));
        assertFalse(command.equals(new MarkPaymentUnpaidCommand(FIRST, SECOND, SECOND)));
    }

    @Test
    public void toStringMethod() {
        MarkPaymentUnpaidCommand command = new MarkPaymentUnpaidCommand(FIRST, SECOND, THIRD);
        String expected = MarkPaymentUnpaidCommand.class.getCanonicalName() + "{clientIndex=" + FIRST
                + ", gigIndex=" + SECOND + ", paymentIndex=" + THIRD + "}";
        assertEquals(expected, command.toString());
    }

    private ModelManager createModel() {
        ModelManager model = new ModelManager();
        model.addClient(ALICE);
        model.addClient(BENSON);
        return model;
    }

    private Gig addGig(ModelManager model, Client client) {
        Gig gig = new Gig(client, new GigTitle("Website redesign"), GigStatus.NOT_STARTED,
                new Deadline("2027-12-31"), new Fee("1250"));
        model.addGig(gig);
        return model.getGigList().get(model.getGigList().size() - 1);
    }
}
