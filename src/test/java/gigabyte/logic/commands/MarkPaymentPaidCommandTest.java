package gigabyte.logic.commands;

import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static gigabyte.testutil.TypicalModels.addGig;
import static gigabyte.testutil.TypicalModels.createModel;
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
import gigabyte.model.client.ClientNameContainsKeywordsPredicate;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.PaymentObligation;

public class MarkPaymentPaidCommandTest {
    private static final Index FIRST = Index.fromOneBased(1);
    private static final Index SECOND = Index.fromOneBased(2);
    private static final Index THIRD = Index.fromOneBased(3);
    private static final Fee AMOUNT = new Fee("50.25");
    private static final Deadline DUE_DATE = new Deadline("2027-01-31");

    @Test
    public void execute_success_marksOnlySelectedPaymentAndPreservesDetails() throws Exception {
        ModelManager model = createModel();
        Gig gig = addGig(model, ALICE);
        model.addPaymentObligation(new PaymentObligation(gig, new Fee("25"), new Deadline("2027-01-01"), false));
        model.addPaymentObligation(new PaymentObligation(gig, AMOUNT, DUE_DATE, false));
        PaymentObligation selectedPayment = model.getPaymentObligationList().get(1);
        PaymentObligation firstPayment = model.getPaymentObligationList().get(0);
        GigabyteData before = new GigabyteData(model.getGigabyteData());

        CommandResult result = new MarkPaymentPaidCommand(FIRST, FIRST, SECOND).execute(model);

        PaymentObligation updatedPayment = model.getPaymentObligationList().get(1);
        assertTrue(updatedPayment.isPaid());
        assertNotSame(selectedPayment, updatedPayment);
        assertFalse(model.getPaymentObligationList().get(0).isPaid());
        assertEquals(AMOUNT, updatedPayment.getAmount());
        assertEquals(DUE_DATE, updatedPayment.getDueDate());
        assertSame(selectedPayment.getGig(), updatedPayment.getGig());
        assertEquals(firstPayment, model.getPaymentObligationList().get(0));
        assertEquals(before.getClientList(), model.getGigabyteData().getClientList());
        assertEquals(before.getGigList(), model.getGigabyteData().getGigList());
        assertEquals("Marked payment obligation for Alice Pauline, gig 1, payment 2 as paid: Amount: $50.25; "
                + "Due date: 2027-01-31", result.getFeedbackToUser());
    }

    @Test
    public void execute_paymentDisplayOrder_usesDueDateAndRecordedTieOrder() throws Exception {
        ModelManager model = createModel();
        Gig gig = addGig(model, ALICE);
        model.addPaymentObligation(new PaymentObligation(gig, new Fee("300"), new Deadline("2027-03-01"), false));
        model.addPaymentObligation(new PaymentObligation(gig, new Fee("100"), DUE_DATE, false));
        model.addPaymentObligation(new PaymentObligation(gig, new Fee("200"), DUE_DATE, false));

        new MarkPaymentPaidCommand(FIRST, FIRST, SECOND).execute(model);

        assertFalse(model.getPaymentObligationList().get(0).isPaid());
        assertFalse(model.getPaymentObligationList().get(1).isPaid());
        assertTrue(model.getPaymentObligationList().get(2).isPaid());
    }

    @Test
    public void execute_filteredClientList_usesDisplayedClientIndex() throws Exception {
        ModelManager model = createModel();
        Gig aliceGig = addGig(model, ALICE);
        Gig bensonGig = addGig(model, BENSON);
        model.addPaymentObligation(new PaymentObligation(aliceGig, AMOUNT, DUE_DATE, false));
        model.addPaymentObligation(new PaymentObligation(bensonGig, AMOUNT, DUE_DATE, false));
        model.updateFilteredClientList(new ClientNameContainsKeywordsPredicate(List.of("Benson")));

        new MarkPaymentPaidCommand(FIRST, FIRST, FIRST).execute(model);

        assertFalse(model.getPaymentObligationList().get(0).isPaid());
        assertTrue(model.getPaymentObligationList().get(1).isPaid());
        assertEquals(List.of(BENSON), model.getFilteredClientList());
    }

    @Test
    public void execute_duplicateLookingPayments_marksOnlySelectedOccurrence() throws Exception {
        ModelManager model = createModel();
        Gig gig = addGig(model, ALICE);
        model.addPaymentObligation(new PaymentObligation(gig, AMOUNT, DUE_DATE, false));
        model.addPaymentObligation(new PaymentObligation(gig, AMOUNT, DUE_DATE, false));
        PaymentObligation selectedSecond = model.getPaymentObligationList().get(1);
        PaymentObligation first = model.getPaymentObligationList().get(0);

        new MarkPaymentPaidCommand(FIRST, FIRST, SECOND).execute(model);

        assertFalse(model.getPaymentObligationList().get(0).isPaid());
        assertTrue(model.getPaymentObligationList().get(1).isPaid());
        assertSame(first, model.getPaymentObligationList().get(0));
        assertNotSame(selectedSecond, model.getPaymentObligationList().get(1));
        assertSame(selectedSecond.getGig(), model.getPaymentObligationList().get(1).getGig());
    }

    @Test
    public void execute_alreadyPaid_throwsAndLeavesDataUnchanged() {
        ModelManager model = createModel();
        Gig gig = addGig(model, ALICE);
        model.addPaymentObligation(new PaymentObligation(gig, AMOUNT, DUE_DATE, true));
        GigabyteData before = new GigabyteData(model.getGigabyteData());
        PaymentObligation alreadyPaid = model.getPaymentObligationList().get(0);

        assertThrows(CommandException.class, MarkPaymentPaidCommand.MESSAGE_ALREADY_PAID, () ->
                new MarkPaymentPaidCommand(FIRST, FIRST, FIRST).execute(model));

        assertEquals(before, model.getGigabyteData());
        assertSame(alreadyPaid, model.getPaymentObligationList().get(0));
    }

    @Test
    public void execute_invalidIndexes_throwsAndLeavesDataUnchanged() {
        ModelManager model = createModel();
        Gig gig = addGig(model, ALICE);
        model.addPaymentObligation(new PaymentObligation(gig, AMOUNT, DUE_DATE, false));
        GigabyteData before = new GigabyteData(model.getGigabyteData());

        // EP: client index beyond the displayed client list
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX, () ->
                new MarkPaymentPaidCommand(THIRD, FIRST, FIRST).execute(model));

        // EP: gig index beyond the client's gigs
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_GIG_INDEX, () ->
                new MarkPaymentPaidCommand(FIRST, SECOND, FIRST).execute(model));

        // EP: payment index beyond the gig's payments
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_PAYMENT_INDEX, () ->
                new MarkPaymentPaidCommand(FIRST, FIRST, SECOND).execute(model));

        assertEquals(before, model.getGigabyteData());
    }

    @Test
    public void equals_comparesAllIndexes() {
        MarkPaymentPaidCommand command = new MarkPaymentPaidCommand(FIRST, SECOND, THIRD);
        // same values or same object -> returns true
        assertEquals(command, new MarkPaymentPaidCommand(FIRST, SECOND, THIRD));
        assertTrue(command.equals(command));

        // null or a different type -> returns false
        assertFalse(command.equals(null));
        assertFalse(command.equals("markpaid"));

        // different client, gig, or payment index -> returns false
        assertFalse(command.equals(new MarkPaymentPaidCommand(SECOND, SECOND, THIRD)));
        assertFalse(command.equals(new MarkPaymentPaidCommand(FIRST, FIRST, THIRD)));
        assertFalse(command.equals(new MarkPaymentPaidCommand(FIRST, SECOND, SECOND)));
    }

    @Test
    public void toStringMethod() {
        MarkPaymentPaidCommand command = new MarkPaymentPaidCommand(FIRST, SECOND, THIRD);
        String expected = MarkPaymentPaidCommand.class.getCanonicalName() + "{clientIndex=" + FIRST
                + ", gigIndex=" + SECOND + ", paymentIndex=" + THIRD + "}";
        assertEquals(expected, command.toString());
    }
}
