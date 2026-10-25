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
import gigabyte.model.client.ClientNameContainsKeywordsPredicate;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;
import gigabyte.model.gig.PaymentObligation;
import gigabyte.model.gig.exceptions.GigHasPaymentsException;

public class DeleteGigCommandTest {
    private static final Index FIRST = Index.fromOneBased(1);
    private static final Index SECOND = Index.fromOneBased(2);
    private static final Index THIRD = Index.fromOneBased(3);

    @Test
    public void execute_allStatuses_deletesOnlyIndexedGigIncludingIdenticalGigs() throws Exception {
        for (GigStatus status : GigStatus.values()) {
            ModelManager model = createModel();
            Gig first = model.getGigList().get(0);
            Gig otherClientGig = model.getGigList().get(1);
            Gig target = model.getGigList().get(2);
            model.setGig(target, target.withStatus(status));
            model.addPaymentObligation(
                    new PaymentObligation(first, new Fee("50"), new Deadline("2099-02-28"), true));
            PaymentObligation payment = model.getPaymentObligationList().get(0);

            CommandResult result = new DeleteGigCommand(FIRST, SECOND).execute(model);

            assertEquals(2, model.getGigList().size());
            assertSame(first, model.getGigList().get(0));
            assertSame(otherClientGig, model.getGigList().get(1));
            assertSame(payment, model.getPaymentObligationList().get(0));
            assertEquals(List.of(ALICE, BENSON), model.getFilteredClientList());
            assertEquals("Deleted gig for Alice Pauline, gig 2: Website", result.getFeedbackToUser());
        }
    }

    @Test
    public void execute_filteredClients_usesDisplayedIndexAndAllowsSubsequentClientDeletion() throws Exception {
        ModelManager model = createModel();
        model.updateFilteredClientList(new ClientNameContainsKeywordsPredicate(List.of("Benson")));

        new DeleteGigCommand(FIRST, FIRST).execute(model);

        assertEquals(List.of(BENSON), model.getFilteredClientList());
        assertTrue(model.getGigList().stream().allMatch(gig -> gig.getClient().hasSameUid(ALICE)));
        new DeleteClientCommand(FIRST).execute(model);
        assertTrue(model.getFilteredClientList().isEmpty());
    }

    @Test
    public void execute_paidOrUnpaidPayments_rejectsAndPreservesRecords() {
        for (boolean isPaid : new boolean[] {false, true}) {
            ModelManager model = createModel();
            model.addPaymentObligation(new PaymentObligation(model.getGigList().get(0), new Fee("50"),
                    new Deadline("2099-02-28"), isPaid));
            GigabyteData before = new GigabyteData(model.getGigabyteData());

            assertThrows(CommandException.class, GigHasPaymentsException.MESSAGE, () ->
                    new DeleteGigCommand(FIRST, FIRST).execute(model));

            assertEquals(before, model.getGigabyteData());
        }
    }

    @Test
    public void execute_invalidIndexes_rejectsWithoutChangingRecords() {
        ModelManager model = createModel();
        GigabyteData before = new GigabyteData(model.getGigabyteData());

        // EP: client index beyond the displayed client list
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX, () ->
                new DeleteGigCommand(THIRD, FIRST).execute(model));

        // EP: gig index beyond the client's gigs
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_GIG_INDEX, () ->
                new DeleteGigCommand(FIRST, THIRD).execute(model));
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_GIG_INDEX, () ->
                new DeleteGigCommand(SECOND, SECOND).execute(model));
        assertEquals(before, model.getGigabyteData());

        // EP: no clients displayed
        model.updateFilteredClientList(client -> false);
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX, () ->
                new DeleteGigCommand(FIRST, FIRST).execute(model));
        assertEquals(before, model.getGigabyteData());
    }

    @Test
    public void constructorAndExecute_nullArguments_rejects() {
        assertThrows(NullPointerException.class, () -> new DeleteGigCommand(null, FIRST)); // client index
        assertThrows(NullPointerException.class, () -> new DeleteGigCommand(FIRST, null)); // gig index
        assertThrows(NullPointerException.class, () -> new DeleteGigCommand(FIRST, FIRST).execute(null)); // model
    }

    @Test
    public void equalsAndToString_useBothIndexes() {
        DeleteGigCommand command = new DeleteGigCommand(FIRST, SECOND);

        // same values or same object -> returns true
        assertEquals(command, new DeleteGigCommand(FIRST, SECOND));
        assertTrue(command.equals(command));

        // null or a different type -> returns false
        assertFalse(command.equals(null));
        assertFalse(command.equals("deletegig"));

        // different client or gig index -> returns false
        assertFalse(command.equals(new DeleteGigCommand(SECOND, SECOND)));
        assertFalse(command.equals(new DeleteGigCommand(FIRST, FIRST)));

        assertEquals(DeleteGigCommand.class.getCanonicalName()
                + "{clientIndex=" + FIRST + ", gigIndex=" + SECOND + "}", command.toString());
    }

    private ModelManager createModel() {
        ModelManager model = new ModelManager();
        model.addClient(ALICE);
        model.addClient(BENSON);
        Gig gig = new Gig(ALICE, new GigTitle("Website"), GigStatus.NOT_STARTED,
                new Deadline("2099-01-31"), new Fee("100"));
        model.addGig(gig);
        model.addGig(gig.withNewUid().withClient(BENSON));
        model.addGig(gig);
        return model;
    }
}
