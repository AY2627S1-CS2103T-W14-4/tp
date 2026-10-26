package gigabyte.logic.commands;

import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import gigabyte.model.GigabyteData;
import gigabyte.model.Model;
import gigabyte.model.ModelManager;
import gigabyte.model.client.ClientNameContainsKeywordsPredicate;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;
import gigabyte.model.gig.PaymentObligation;

public class FilterGigsCommandTest {
    @Test
    public void execute_multipleClients_countsOnlyMatchingGigsAndPreservesRecords() {
        Model model = new ModelManager();
        model.addClient(ALICE);
        model.addClient(BENSON);
        Gig aliceGig = new Gig(ALICE, new GigTitle("Website"), GigStatus.IN_PROGRESS,
                new Deadline("2027-01-31"), new Fee("100"));
        model.addGig(aliceGig);
        model.addGig(new Gig(BENSON, new GigTitle("Logo"), GigStatus.IN_PROGRESS,
                new Deadline("2027-02-28"), new Fee("200")));
        model.addGig(new Gig(ALICE, new GigTitle("Poster"), GigStatus.COMPLETED,
                new Deadline("2027-03-31"), new Fee("300")));
        model.addPaymentObligation(new PaymentObligation(aliceGig, new Fee("50"),
                new Deadline("2027-02-28"), false));
        model.updateFilteredClientList(new ClientNameContainsKeywordsPredicate(List.of("Alice")));
        GigabyteData before = new GigabyteData(model.getGigabyteData());

        CommandResult result = new FilterGigsCommand(GigStatus.IN_PROGRESS).execute(model);

        assertEquals(new CommandResult("2 gigs with status IN_PROGRESS.", GigStatus.IN_PROGRESS), result);
        assertEquals(before, model.getGigabyteData());
        assertEquals(List.of(ALICE), model.getFilteredClientList());
        assertEquals(new CommandResult("0 gigs with status NOT_STARTED.", GigStatus.NOT_STARTED),
                new FilterGigsCommand(GigStatus.NOT_STARTED).execute(model));
        assertEquals(before, model.getGigabyteData());
    }

    @Test
    public void execute_emptyModel_returnsZeroMatches() {
        assertEquals(new CommandResult("0 gigs with status COMPLETED.", GigStatus.COMPLETED),
                new FilterGigsCommand(GigStatus.COMPLETED).execute(new ModelManager()));
    }

    @Test
    public void nullArguments_throwNullPointerException() {
        assertThrows(NullPointerException.class, () -> new FilterGigsCommand(null));
        assertThrows(NullPointerException.class, () -> new FilterGigsCommand(GigStatus.COMPLETED).execute(null));
    }

    @Test
    public void equalsAndToString_compareStatus() {
        FilterGigsCommand command = new FilterGigsCommand(GigStatus.IN_PROGRESS);
        // same object or same status -> returns true
        assertTrue(command.equals(command));
        assertTrue(command.equals(new FilterGigsCommand(GigStatus.IN_PROGRESS)));

        // different status -> returns false
        assertFalse(command.equals(new FilterGigsCommand(GigStatus.COMPLETED)));

        // null or a different type -> returns false
        assertFalse(command.equals(null));
        assertFalse(command.equals("IN_PROGRESS"));

        assertEquals(FilterGigsCommand.class.getCanonicalName() + "{status=IN_PROGRESS}", command.toString());
    }
}
