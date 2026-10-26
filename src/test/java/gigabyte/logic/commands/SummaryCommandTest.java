package gigabyte.logic.commands;

import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import gigabyte.model.ModelManager;
import gigabyte.model.client.ClientNameContainsKeywordsPredicate;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;
import gigabyte.model.gig.PaymentObligation;

public class SummaryCommandTest {
    private static final Clock TEST_CLOCK = Clock.fixed(
            Instant.parse("2026-10-21T00:00:00Z"), ZoneId.of("Asia/Singapore"));

    @Test
    public void execute_emptyModel_returnsZeroSummary() {
        CommandResult result = new SummaryCommand(TEST_CLOCK).execute(new ModelManager());

        assertEquals("Clients: 0\n"
                + "Gigs: 0 total\n"
                + "  NOT_STARTED: 0\n"
                + "  IN_PROGRESS: 0\n"
                + "  COMPLETED: 0\n"
                + "Payments:\n"
                + "  Outstanding: 0 ($0.00)\n"
                + "  Of which overdue: 0 ($0.00)\n"
                + "  Marked paid: 0 ($0.00)", result.getFeedbackToUser());
    }

    @Test
    public void execute_mixedData_usesAllRecordsAndPreservesClientFilter() {
        ModelManager model = new ModelManager();
        model.addClient(ALICE);
        model.addClient(BENSON);
        Gig first = addGig(model, ALICE, "Website", GigStatus.NOT_STARTED);
        Gig second = addGig(model, BENSON, "Logo", GigStatus.IN_PROGRESS);
        addGig(model, ALICE, "Branding", GigStatus.IN_PROGRESS);
        addGig(model, BENSON, "Portfolio", GigStatus.COMPLETED);
        model.addPaymentObligation(createPayment(first, "499.99", "2026-10-20", false));
        model.addPaymentObligation(createPayment(second, "100.01", "2026-10-21", false));
        model.addPaymentObligation(createPayment(second, "2000", "2026-10-22", false));
        model.addPaymentObligation(createPayment(first, "300.50", "2026-10-01", true));
        model.addPaymentObligation(createPayment(second, "50.25", "2026-11-01", true));
        model.updateFilteredClientList(new ClientNameContainsKeywordsPredicate(List.of("Alice")));
        List<?> filteredClientsBefore = List.copyOf(model.getFilteredClientList());

        CommandResult result = new SummaryCommand(TEST_CLOCK).execute(model);

        assertEquals("Clients: 2\n"
                + "Gigs: 4 total\n"
                + "  NOT_STARTED: 1\n"
                + "  IN_PROGRESS: 2\n"
                + "  COMPLETED: 1\n"
                + "Payments:\n"
                + "  Outstanding: 3 ($2,600.00)\n"
                + "  Of which overdue: 1 ($499.99)\n"
                + "  Marked paid: 2 ($350.75)", result.getFeedbackToUser());
        assertEquals(filteredClientsBefore, List.copyOf(model.getFilteredClientList()));
    }

    @Test
    public void execute_largeAmounts_preservesExactCents() {
        ModelManager model = new ModelManager();
        model.addClient(ALICE);
        Gig gig = addGig(model, ALICE, "Large contract", GigStatus.IN_PROGRESS);
        model.addPaymentObligation(createPayment(gig, "99999999999999999999999999.99", "2026-10-20", false));
        model.addPaymentObligation(createPayment(gig, "10000000000000000000000000.01", "2026-10-21", true));

        CommandResult result = new SummaryCommand(TEST_CLOCK).execute(model);

        assertEquals("Clients: 1\n"
                + "Gigs: 1 total\n"
                + "  NOT_STARTED: 0\n"
                + "  IN_PROGRESS: 1\n"
                + "  COMPLETED: 0\n"
                + "Payments:\n"
                + "  Outstanding: 1 ($99,999,999,999,999,999,999,999,999.99)\n"
                + "  Of which overdue: 1 ($99,999,999,999,999,999,999,999,999.99)\n"
                + "  Marked paid: 1 ($10,000,000,000,000,000,000,000,000.01)",
                result.getFeedbackToUser());
    }

    private static Gig addGig(ModelManager model, gigabyte.model.client.Client client,
            String title, GigStatus status) {
        Gig gig = new Gig(client, new GigTitle(title), status,
                new Deadline("2027-01-01"), new Fee("100"));
        model.addGig(gig);
        return model.getGigList().get(model.getGigList().size() - 1);
    }

    private static PaymentObligation createPayment(Gig gig, String amount, String dueDate, boolean isPaid) {
        return new PaymentObligation(gig, new Fee(amount), new Deadline(dueDate), isPaid);
    }
}
