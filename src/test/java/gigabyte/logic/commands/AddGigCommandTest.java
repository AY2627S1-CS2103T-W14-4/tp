package gigabyte.logic.commands;

import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static gigabyte.testutil.TypicalIndexes.INDEX_FIRST_CLIENT;
import static gigabyte.testutil.TypicalIndexes.INDEX_SECOND_CLIENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import gigabyte.logic.Messages;
import gigabyte.logic.commands.exceptions.CommandException;
import gigabyte.model.ModelManager;
import gigabyte.model.client.ClientNameContainsKeywordsPredicate;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;

public class AddGigCommandTest {
    private static final Deadline DEADLINE = new Deadline("2027-01-31");
    private static final Fee FEE = new Fee("1250.50");

    @Test
    public void execute_validClient_addsGigAndKeepsClientList() throws Exception {
        ModelManager model = new ModelManager();
        model.addClient(ALICE);
        model.addClient(BENSON);
        model.addGig(new Gig(BENSON, GigStatus.COMPLETED, DEADLINE, FEE));

        CommandResult result = new AddGigCommand(INDEX_FIRST_CLIENT, GigStatus.NOT_STARTED, DEADLINE, FEE)
                .execute(model);

        assertEquals(new Gig(ALICE, GigStatus.NOT_STARTED, DEADLINE, FEE), model.getGigList().get(1));
        assertEquals(2, model.getGigList().size());
        assertEquals(List.of(ALICE, BENSON), model.getFilteredClientList());
        assertEquals(String.format(AddGigCommand.MESSAGE_SUCCESS, ALICE.getName(), GigStatus.NOT_STARTED,
                DEADLINE, FEE), result.getFeedbackToUser());
    }

    @Test
    public void execute_filteredList_usesDisplayedIndexAndKeepsFilter() throws Exception {
        ModelManager model = new ModelManager();
        model.addClient(ALICE);
        model.addClient(BENSON);
        model.updateFilteredClientList(new ClientNameContainsKeywordsPredicate(List.of("Benson")));

        new AddGigCommand(INDEX_FIRST_CLIENT, GigStatus.IN_PROGRESS, DEADLINE, FEE).execute(model);

        assertEquals(List.of(new Gig(BENSON, GigStatus.IN_PROGRESS, DEADLINE, FEE)), model.getGigList());
        assertEquals(List.of(BENSON), model.getFilteredClientList());
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX, () ->
                new AddGigCommand(INDEX_SECOND_CLIENT, GigStatus.IN_PROGRESS, DEADLINE, FEE).execute(model));
        assertEquals(1, model.getGigList().size());
    }

    @Test
    public void execute_emptyClientList_throwsCommandException() {
        ModelManager model = new ModelManager();
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX, () ->
                new AddGigCommand(INDEX_FIRST_CLIENT, GigStatus.NOT_STARTED, DEADLINE, FEE).execute(model));
        assertTrue(model.getGigList().isEmpty());
    }

    @Test
    public void equals_differentFields_returnsFalse() {
        AddGigCommand command = new AddGigCommand(INDEX_FIRST_CLIENT, GigStatus.NOT_STARTED, DEADLINE, FEE);
        assertEquals(command, new AddGigCommand(INDEX_FIRST_CLIENT, GigStatus.NOT_STARTED, DEADLINE, FEE));
        assertNotEquals(command, new AddGigCommand(INDEX_SECOND_CLIENT, GigStatus.NOT_STARTED, DEADLINE, FEE));
        assertNotEquals(command, new AddGigCommand(INDEX_FIRST_CLIENT, GigStatus.COMPLETED, DEADLINE, FEE));
        assertNotEquals(command, new AddGigCommand(INDEX_FIRST_CLIENT, GigStatus.NOT_STARTED,
                new Deadline("2027-02-28"), FEE));
        assertNotEquals(command, new AddGigCommand(INDEX_FIRST_CLIENT, GigStatus.NOT_STARTED, DEADLINE,
                new Fee("100.00")));
    }

    @Test
    public void equals_sameObject_returnsTrue() {
        AddGigCommand command =
                new AddGigCommand(INDEX_FIRST_CLIENT, GigStatus.NOT_STARTED, DEADLINE, FEE);

        assertTrue(command.equals(command));
    }

    @Test
    public void equals_nullOrDifferentType_returnsFalse() {
        AddGigCommand command =
                new AddGigCommand(INDEX_FIRST_CLIENT, GigStatus.NOT_STARTED, DEADLINE, FEE);

        assertFalse(command.equals(null));
        assertFalse(command.equals("test"));
    }

    @Test
    public void toStringMethod() {
        AddGigCommand command =
                new AddGigCommand(INDEX_FIRST_CLIENT, GigStatus.NOT_STARTED, DEADLINE, FEE);
        String expected = AddGigCommand.class.getCanonicalName()
                + "{clientIndex=" + INDEX_FIRST_CLIENT
                + ", status=" + GigStatus.NOT_STARTED
                + ", deadline=" + DEADLINE
                + ", fee=" + FEE + "}";

        assertEquals(expected, command.toString());
    }
}
