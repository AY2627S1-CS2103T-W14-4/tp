package gigabyte.logic.commands;

import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

public class OutstandingCommandTest {
    @Test
    public void execute_multipleClients_excludesPaidAndPreservesRecordsAndClientFilter() {
        Model model = new ModelManager();
        model.addClient(ALICE);
        model.addClient(BENSON);
        Gig first = new Gig(ALICE, new GigTitle("Website"), GigStatus.COMPLETED,
                new Deadline("2027-01-31"), new Fee("100"));
        Gig second = new Gig(BENSON, new GigTitle("Logo"), GigStatus.IN_PROGRESS,
                new Deadline("2027-02-28"), new Fee("200"));
        model.addGig(first);
        model.addGig(second);
        model.addPaymentObligation(new PaymentObligation(first, new Fee("50"), new Deadline("2027-03-01"), false));
        model.addPaymentObligation(new PaymentObligation(second, new Fee("75"), new Deadline("2027-01-01"), false));
        model.addPaymentObligation(new PaymentObligation(first, new Fee("25"), new Deadline("2027-01-01"), true));
        model.updateFilteredClientList(new ClientNameContainsKeywordsPredicate(List.of("Alice")));
        GigabyteData before = new GigabyteData(model.getGigabyteData());

        assertEquals(CommandResult.forOutstandingPayments("2 outstanding payment obligations."),
                new OutstandingCommand().execute(model));
        assertEquals(before, model.getGigabyteData());
        assertEquals(List.of(ALICE), model.getFilteredClientList());
    }

    @Test
    public void execute_emptyModel_returnsZero() {
        assertEquals(CommandResult.forOutstandingPayments("0 outstanding payment obligations."),
                new OutstandingCommand().execute(new ModelManager()));
        assertThrows(NullPointerException.class, () -> new OutstandingCommand().execute(null));
    }
}
