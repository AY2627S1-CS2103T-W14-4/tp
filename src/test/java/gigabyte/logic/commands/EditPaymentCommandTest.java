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
import java.util.Optional;

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
import gigabyte.model.gig.PaymentObligationOrdering;

public class EditPaymentCommandTest {
    private static final Index FIRST = Index.fromOneBased(1);
    private static final Index SECOND = Index.fromOneBased(2);
    private static final Index THIRD = Index.fromOneBased(3);
    private static final Fee AMOUNT = new Fee("50.25");
    private static final Deadline DUE_DATE = new Deadline("2027-01-31");

    @Test
    public void execute_amountOnly_preservesDueDateGigAndPaidState() throws Exception {
        ModelManager model = createModel();
        Gig gig = addGig(model, ALICE);
        model.addPaymentObligation(new PaymentObligation(gig, AMOUNT, DUE_DATE, true));
        PaymentObligation selected = model.getPaymentObligationList().get(0);

        CommandResult result = new EditPaymentCommand(FIRST, FIRST, FIRST,
                Optional.of(new Fee("650")), Optional.empty()).execute(model);

        PaymentObligation edited = model.getPaymentObligationList().get(0);
        assertEquals(new Fee("650"), edited.getAmount());
        assertEquals(DUE_DATE, edited.getDueDate());
        assertTrue(edited.isPaid());
        assertSame(selected.getGig(), edited.getGig());
        assertEquals("Edited payment obligation for Alice Pauline, gig 1, payment 1: Amount: 650.00; "
                + "Due date: 2027-01-31", result.getFeedbackToUser());
    }

    @Test
    public void execute_dueDateOnly_resolvesDisplayedIndexBeforeReordering() throws Exception {
        ModelManager model = createModel();
        Gig gig = addGig(model, ALICE);
        model.addPaymentObligation(new PaymentObligation(gig, new Fee("300"),
                new Deadline("2027-03-01"), false));
        model.addPaymentObligation(new PaymentObligation(gig, new Fee("100"),
                new Deadline("2027-01-01"), false));
        model.addPaymentObligation(new PaymentObligation(gig, new Fee("200"),
                new Deadline("2027-02-01"), false));
        PaymentObligation selected = model.getPaymentObligationList().get(2);

        new EditPaymentCommand(FIRST, FIRST, SECOND, Optional.empty(),
                Optional.of(new Deadline("2026-12-01"))).execute(model);

        PaymentObligation edited = model.getPaymentObligationList().get(2);
        assertNotSame(selected, edited);
        assertEquals(new Fee("200"), edited.getAmount());
        assertEquals(new Deadline("2026-12-01"), edited.getDueDate());
        assertEquals(List.of(edited, model.getPaymentObligationList().get(1),
                model.getPaymentObligationList().get(0)),
                PaymentObligationOrdering.forGig(model.getPaymentObligationList(), gig));
    }

    @Test
    public void execute_duplicatePayments_editsSelectedOccurrence() throws Exception {
        ModelManager model = createModel();
        Gig gig = addGig(model, ALICE);
        model.addPaymentObligation(new PaymentObligation(gig, AMOUNT, DUE_DATE, false));
        model.addPaymentObligation(new PaymentObligation(gig, AMOUNT, DUE_DATE, false));
        PaymentObligation first = model.getPaymentObligationList().get(0);
        PaymentObligation selectedSecond = model.getPaymentObligationList().get(1);

        new EditPaymentCommand(FIRST, FIRST, SECOND, Optional.of(new Fee("75")),
                Optional.of(new Deadline("2027-02-15"))).execute(model);

        PaymentObligation edited = model.getPaymentObligationList().get(1);
        assertSame(first, model.getPaymentObligationList().get(0));
        assertNotSame(selectedSecond, edited);
        assertEquals(new Fee("75"), edited.getAmount());
        assertEquals(new Deadline("2027-02-15"), edited.getDueDate());
        assertFalse(edited.isPaid());
        assertSame(selectedSecond.getGig(), edited.getGig());
    }

    @Test
    public void execute_filteredClientList_usesDisplayedClientIndex() throws Exception {
        ModelManager model = createModel();
        Gig aliceGig = addGig(model, ALICE);
        Gig bensonGig = addGig(model, BENSON);
        model.addPaymentObligation(new PaymentObligation(aliceGig, AMOUNT, DUE_DATE, false));
        model.addPaymentObligation(new PaymentObligation(bensonGig, AMOUNT, DUE_DATE, false));
        model.updateFilteredClientList(new ClientNameContainsKeywordsPredicate(List.of("Benson")));

        new EditPaymentCommand(FIRST, FIRST, FIRST, Optional.of(new Fee("75")),
                Optional.empty()).execute(model);

        assertEquals(AMOUNT, model.getPaymentObligationList().get(0).getAmount());
        assertEquals(new Fee("75"), model.getPaymentObligationList().get(1).getAmount());
        assertEquals(List.of(BENSON), model.getFilteredClientList());
    }

    @Test
    public void execute_invalidIndexes_throwsAndLeavesDataUnchanged() {
        ModelManager model = createModel();
        Gig gig = addGig(model, ALICE);
        model.addPaymentObligation(new PaymentObligation(gig, AMOUNT, DUE_DATE, false));
        GigabyteData before = new GigabyteData(model.getGigabyteData());

        // EP: client index beyond the displayed client list
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX, () ->
                amountEdit(THIRD, FIRST, FIRST).execute(model));

        // EP: gig index beyond the client's gigs
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_GIG_INDEX, () ->
                amountEdit(FIRST, SECOND, FIRST).execute(model));

        // EP: payment index beyond the gig's payments
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_PAYMENT_INDEX, () ->
                amountEdit(FIRST, FIRST, SECOND).execute(model));

        assertEquals(before, model.getGigabyteData());
    }

    @Test
    public void execute_resultingNoOp_throwsAndLeavesDataUnchanged() {
        ModelManager model = createModel();
        Gig gig = addGig(model, ALICE);
        model.addPaymentObligation(new PaymentObligation(gig, AMOUNT, DUE_DATE, false));
        GigabyteData before = new GigabyteData(model.getGigabyteData());
        PaymentObligation selected = model.getPaymentObligationList().get(0);

        assertThrows(CommandException.class, EditPaymentCommand.MESSAGE_PAYMENT_NOT_EDITED, () ->
                new EditPaymentCommand(FIRST, FIRST, FIRST, Optional.of(AMOUNT),
                        Optional.of(DUE_DATE)).execute(model));

        assertEquals(before, model.getGigabyteData());
        assertSame(selected, model.getPaymentObligationList().get(0));
    }

    @Test
    public void equals_comparesIndexesAndEditedFields() {
        EditPaymentCommand command = amountEdit(FIRST, SECOND, THIRD);

        // same values or same object -> returns true
        assertEquals(command, amountEdit(FIRST, SECOND, THIRD));
        assertTrue(command.equals(command));

        // null or a different type -> returns false
        assertFalse(command.equals(null));
        assertFalse(command.equals("editpayment"));

        // different client, gig, or payment index -> returns false
        assertFalse(command.equals(amountEdit(SECOND, SECOND, THIRD)));
        assertFalse(command.equals(amountEdit(FIRST, FIRST, THIRD)));
        assertFalse(command.equals(amountEdit(FIRST, SECOND, SECOND)));

        // different edited field -> returns false
        assertFalse(command.equals(new EditPaymentCommand(FIRST, SECOND, THIRD, Optional.empty(),
                Optional.of(DUE_DATE))));
    }

    @Test
    public void toStringMethod() {
        EditPaymentCommand command = amountEdit(FIRST, SECOND, THIRD);
        String expected = EditPaymentCommand.class.getCanonicalName() + "{clientIndex=" + FIRST
                + ", gigIndex=" + SECOND + ", paymentIndex=" + THIRD + ", amount=Optional[50.25]"
                + ", dueDate=Optional.empty}";
        assertEquals(expected, command.toString());
    }

    private static EditPaymentCommand amountEdit(Index clientIndex, Index gigIndex, Index paymentIndex) {
        return new EditPaymentCommand(clientIndex, gigIndex, paymentIndex, Optional.of(AMOUNT), Optional.empty());
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
