package gigabyte.logic.commands;

import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static gigabyte.testutil.TypicalIndexes.INDEX_FIRST_CLIENT;
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

public class SetGigStatusCommandTest {
    private static final Index FIRST = INDEX_FIRST_CLIENT;
    private static final Index SECOND = Index.fromOneBased(2);
    private static final Deadline DEADLINE = new Deadline("2027-01-31");

    @Test
    public void execute_eachSupportedStatus_updatesOnlySelectedGigAndRelinksPayments() throws Exception {
        for (GigStatus requestedStatus : GigStatus.values()) {
            ModelManager model = createModel();
            Gig selectedGig = createGig(ALICE, "100", GigStatus.NOT_STARTED);
            if (requestedStatus == GigStatus.NOT_STARTED) {
                selectedGig = createGig(ALICE, "100", GigStatus.IN_PROGRESS);
            }
            model.addGig(selectedGig);
            model.addGig(createGig(BENSON, "200", GigStatus.NOT_STARTED));
            model.addGig(createGig(ALICE, "300", GigStatus.COMPLETED));
            model.addPaymentObligation(new PaymentObligation(model.getGigList().get(0), new Fee("50"),
                    DEADLINE, false));
            Gig unchangedSecondGig = model.getGigList().get(1);
            Gig unchangedThirdGig = model.getGigList().get(2);

            CommandResult result = new SetGigStatusCommand(FIRST, FIRST, requestedStatus).execute(model);

            Gig updatedGig = model.getGigList().get(0);
            assertEquals(requestedStatus, updatedGig.getStatus());
            assertEquals(selectedGig.getUid(), updatedGig.getUid());
            assertSame(selectedGig.getClient(), updatedGig.getClient());
            assertEquals(selectedGig.getTitle(), updatedGig.getTitle());
            assertEquals(selectedGig.getDeadline(), updatedGig.getDeadline());
            assertEquals(selectedGig.getAgreedFee(), updatedGig.getAgreedFee());
            assertSame(updatedGig, model.getPaymentObligationList().get(0).getGig());
            assertSame(unchangedSecondGig, model.getGigList().get(1));
            assertSame(unchangedThirdGig, model.getGigList().get(2));
            assertEquals(String.format(SetGigStatusCommand.MESSAGE_SUCCESS, ALICE.getName(), 1, requestedStatus),
                    result.getFeedbackToUser());
        }
    }

    @Test
    public void execute_filteredClientList_usesDisplayedClientIndex() throws Exception {
        ModelManager model = createModel();
        model.addGig(createGig(ALICE, "100", GigStatus.NOT_STARTED));
        model.addGig(createGig(BENSON, "200", GigStatus.NOT_STARTED));
        model.updateFilteredClientList(new ClientNameContainsKeywordsPredicate(List.of("Benson")));

        new SetGigStatusCommand(FIRST, FIRST, GigStatus.COMPLETED).execute(model);

        assertEquals(List.of(BENSON), model.getFilteredClientList());
        assertEquals(GigStatus.NOT_STARTED, model.getGigList().get(0).getStatus());
        assertEquals(GigStatus.COMPLETED, model.getGigList().get(1).getStatus());
    }

    @Test
    public void execute_equalGigs_updatesOnlyIndexedGigAndItsObligation() throws Exception {
        ModelManager model = createModel();
        Gig equalGig = createGig(ALICE, "100", GigStatus.NOT_STARTED);
        model.addGig(equalGig);
        model.addGig(equalGig);
        Gig firstGig = model.getGigList().get(0);
        Gig secondGig = model.getGigList().get(1);
        model.addPaymentObligation(new PaymentObligation(secondGig, new Fee("50"), DEADLINE, false));

        new SetGigStatusCommand(FIRST, SECOND, GigStatus.COMPLETED).execute(model);

        assertSame(firstGig, model.getGigList().get(0));
        assertEquals(GigStatus.NOT_STARTED, model.getGigList().get(0).getStatus());
        assertEquals(secondGig.getUid(), model.getGigList().get(1).getUid());
        assertEquals(GigStatus.COMPLETED, model.getGigList().get(1).getStatus());
        assertSame(model.getGigList().get(1), model.getPaymentObligationList().get(0).getGig());
    }

    @Test
    public void execute_invalidIndexesOrCurrentStatus_leavesDataUnchanged() {
        ModelManager model = createModel();
        model.addGig(createGig(ALICE, "100", GigStatus.NOT_STARTED));
        model.addPaymentObligation(new PaymentObligation(model.getGigList().get(0), new Fee("50"), DEADLINE, false));
        GigabyteData before = new GigabyteData(model.getGigabyteData());

        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX, () ->
                new SetGigStatusCommand(Index.fromOneBased(3), FIRST, GigStatus.COMPLETED).execute(model));
        assertThrows(CommandException.class, SetGigStatusCommand.MESSAGE_INVALID_GIG_INDEX, () ->
                new SetGigStatusCommand(FIRST, SECOND, GigStatus.COMPLETED).execute(model));
        assertThrows(CommandException.class, SetGigStatusCommand.MESSAGE_STATUS_UNCHANGED.formatted(
                GigStatus.NOT_STARTED), () -> new SetGigStatusCommand(FIRST, FIRST, GigStatus.NOT_STARTED)
                .execute(model));

        assertEquals(before, model.getGigabyteData());
        assertSame(model.getGigList().get(0), model.getPaymentObligationList().get(0).getGig());
    }

    @Test
    public void execute_noClients_throwsCommandException() {
        ModelManager model = new ModelManager();

        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX, () ->
                new SetGigStatusCommand(FIRST, FIRST, GigStatus.COMPLETED).execute(model));
        assertTrue(model.getGigList().isEmpty());
    }

    @Test
    public void equals_comparesAllFieldsAndHandlesOtherTypes() {
        SetGigStatusCommand command = new SetGigStatusCommand(FIRST, SECOND, GigStatus.COMPLETED);

        assertTrue(command.equals(command));
        assertFalse(command.equals(null));
        assertFalse(command.equals("setstatus"));
        assertEquals(command, new SetGigStatusCommand(FIRST, SECOND, GigStatus.COMPLETED));
        assertNotEquals(command, new SetGigStatusCommand(SECOND, SECOND, GigStatus.COMPLETED));
        assertNotEquals(command, new SetGigStatusCommand(FIRST, FIRST, GigStatus.COMPLETED));
        assertNotEquals(command, new SetGigStatusCommand(FIRST, SECOND, GigStatus.IN_PROGRESS));
    }

    @Test
    public void toStringMethod() {
        SetGigStatusCommand command = new SetGigStatusCommand(FIRST, SECOND, GigStatus.COMPLETED);
        String expected = SetGigStatusCommand.class.getCanonicalName() + "{clientIndex=" + FIRST
                + ", gigIndex=" + SECOND + ", status=" + GigStatus.COMPLETED + "}";

        assertEquals(expected, command.toString());
    }

    private ModelManager createModel() {
        ModelManager model = new ModelManager();
        model.addClient(ALICE);
        model.addClient(BENSON);
        return model;
    }

    private Gig createGig(Client client, String fee, GigStatus status) {
        return new Gig(client, new GigTitle("Website redesign"), status, DEADLINE, new Fee(fee));
    }
}
