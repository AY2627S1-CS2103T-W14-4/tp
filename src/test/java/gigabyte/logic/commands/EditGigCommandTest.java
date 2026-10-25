package gigabyte.logic.commands;

import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import gigabyte.commons.core.index.Index;
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

public class EditGigCommandTest {
    private static final Index FIRST = Index.fromOneBased(1);
    private static final Index SECOND = Index.fromOneBased(2);
    private static final GigTitle TITLE = new GigTitle("Revised website");
    private static final Deadline DEADLINE = new Deadline("2027-04-30");
    private static final Fee FEE = new Fee("1800");

    @Test
    public void execute_eachFieldCombination_updatesOnlyIndexedGigAndPreservesBothPaymentStates() throws Exception {
        for (int fields = 1; fields < 8; fields++) {
            ModelManager model = createModel();
            Gig original = model.getGigList().get(1);
            Gig untouched = model.getGigList().get(0);
            model.addPaymentObligation(new PaymentObligation(original, new Fee("50"), DEADLINE, true));
            model.addPaymentObligation(new PaymentObligation(original, new Fee("25"), DEADLINE, false));
            List<PaymentObligation> payments = List.copyOf(model.getPaymentObligationList());
            Optional<GigTitle> title = (fields & 1) == 0 ? Optional.empty() : Optional.of(TITLE);
            Optional<Deadline> deadline = (fields & 2) == 0 ? Optional.empty() : Optional.of(DEADLINE);
            Optional<Fee> fee = (fields & 4) == 0 ? Optional.empty() : Optional.of(FEE);

            CommandResult result = new EditGigCommand(FIRST, SECOND, title, deadline, fee).execute(model);

            Gig edited = model.getGigList().get(1);
            assertSame(untouched, model.getGigList().get(0));
            assertEquals(original.getUid(), edited.getUid());
            assertSame(original.getClient(), edited.getClient());
            assertEquals(original.getStatus(), edited.getStatus());
            assertEquals((fields & 1) == 0 ? original.getTitle() : TITLE, edited.getTitle());
            assertEquals((fields & 2) == 0 ? original.getDeadline() : DEADLINE, edited.getDeadline());
            assertEquals((fields & 4) == 0 ? original.getAgreedFee() : FEE, edited.getAgreedFee());
            assertTrue(result.getFeedbackToUser().contains(edited.getTitle().toString()));
            for (int i = 0; i < payments.size(); i++) {
                PaymentObligation payment = model.getPaymentObligationList().get(i);
                assertSame(edited, payment.getGig());
                assertEquals(payments.get(i).getAmount(), payment.getAmount());
                assertEquals(payments.get(i).getDueDate(), payment.getDueDate());
                assertEquals(payments.get(i).isPaid(), payment.isPaid());
            }
        }
    }

    @Test
    public void execute_filteredClients_usesPerClientGigIndexAndKeepsFilter() throws Exception {
        ModelManager model = createModel();
        model.updateFilteredClientList(new ClientNameContainsKeywordsPredicate(List.of("Benson")));

        new EditGigCommand(FIRST, FIRST, Optional.of(TITLE), Optional.empty(), Optional.empty()).execute(model);

        assertEquals(TITLE, model.getGigList().get(2).getTitle());
        assertEquals(new GigTitle("Website"), model.getGigList().get(0).getTitle());
        assertEquals(List.of(BENSON), model.getFilteredClientList());
    }

    @Test
    public void execute_invalidIndexesOrNoOp_preservesRecords() {
        ModelManager model = createModel();
        Gig original = model.getGigList().get(0);
        model.addPaymentObligation(new PaymentObligation(original, new Fee("50"), DEADLINE, false));
        GigabyteData before = new GigabyteData(model.getGigabyteData());

        // EP: client index beyond the displayed client list
        assertThrows(CommandException.class, () -> new EditGigCommand(Index.fromOneBased(3), FIRST,
                Optional.of(TITLE), Optional.empty(), Optional.empty()).execute(model));

        // EP: gig index beyond the client's gigs
        assertThrows(CommandException.class, () -> new EditGigCommand(SECOND, SECOND,
                Optional.of(TITLE), Optional.empty(), Optional.empty()).execute(model));

        // EP: new details are the same as the current ones
        CommandException error = assertThrows(CommandException.class, () -> new EditGigCommand(FIRST, FIRST,
                Optional.of(original.getTitle()), Optional.empty(), Optional.empty()).execute(model));
        assertEquals(EditGigCommand.MESSAGE_NOT_EDITED, error.getMessage());
        assertEquals(before, model.getGigabyteData());
    }

    @Test
    public void constructorAndExecute_invalidArguments_rejected() {
        // EP: no field to edit
        assertThrows(IllegalArgumentException.class, () -> new EditGigCommand(FIRST, FIRST,
                Optional.empty(), Optional.empty(), Optional.empty()));

        // EP: null index or model
        assertThrows(NullPointerException.class, () -> new EditGigCommand(null, FIRST,
                Optional.of(TITLE), Optional.empty(), Optional.empty()));
        assertThrows(NullPointerException.class, () -> new EditGigCommand(FIRST, FIRST,
                Optional.of(TITLE), Optional.empty(), Optional.empty()).execute(null));
    }

    @Test
    public void equalsAndToString_includeIndexesAndDetails() {
        EditGigCommand command = new EditGigCommand(FIRST, SECOND, Optional.of(TITLE),
                Optional.of(DEADLINE), Optional.of(FEE));

        // same object or same values -> returns true
        assertTrue(command.equals(command));
        assertTrue(command.equals(new EditGigCommand(FIRST, SECOND, Optional.of(TITLE),
                Optional.of(DEADLINE), Optional.of(FEE))));

        // null or a different type -> returns false
        assertFalse(command.equals(null));
        assertFalse(command.equals("editgig"));

        // different client or gig index -> returns false
        assertFalse(command.equals(new EditGigCommand(SECOND, SECOND, Optional.of(TITLE),
                Optional.of(DEADLINE), Optional.of(FEE))));
        assertFalse(command.equals(new EditGigCommand(FIRST, FIRST, Optional.of(TITLE),
                Optional.of(DEADLINE), Optional.of(FEE))));

        // one detail left out -> returns false
        for (int field = 0; field < 3; field++) {
            assertFalse(command.equals(new EditGigCommand(FIRST, SECOND,
                    field == 0 ? Optional.empty() : Optional.of(TITLE),
                    field == 1 ? Optional.empty() : Optional.of(DEADLINE),
                    field == 2 ? Optional.empty() : Optional.of(FEE))));
        }
        assertEquals(EditGigCommand.class.getCanonicalName() + "{clientIndex=" + FIRST + ", gigIndex=" + SECOND
                + ", title=Optional[Revised website], deadline=Optional[2027-04-30], fee=Optional[1800.00]}",
                command.toString());
    }

    private ModelManager createModel() {
        ModelManager model = new ModelManager();
        model.addClient(ALICE);
        model.addClient(BENSON);
        Gig gig = new Gig(ALICE, new GigTitle("Website"), GigStatus.IN_PROGRESS,
                new Deadline("2027-01-31"), new Fee("100"));
        model.addGig(gig);
        model.addGig(gig.withNewUid());
        model.addGig(gig.withClient(BENSON).withNewUid());
        return model;
    }
}
