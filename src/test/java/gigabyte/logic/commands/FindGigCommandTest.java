package gigabyte.logic.commands;

import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import gigabyte.model.GigabyteData;
import gigabyte.model.ModelManager;
import gigabyte.model.UserPrefs;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;
import gigabyte.model.gig.GigTitleContainsKeywordsPredicate;

public class FindGigCommandTest {
    @Test
    public void execute_searchesAllClientsAndKeepsSourceOrder() {
        Gig first = gig(ALICE, "Website redesign");
        Gig second = gig(BENSON, "Logo design");
        Gig third = gig(ALICE, "Mobile application");
        GigabyteData data = new GigabyteData();
        data.addClient(ALICE);
        data.addClient(BENSON);
        data.addGig(first);
        data.addGig(second);
        data.addGig(third);
        ModelManager model = new ModelManager(data, new UserPrefs());
        model.updateFilteredClientList(client -> client.equals(ALICE));

        new FindGigCommand(new GigTitleContainsKeywordsPredicate(List.of("design", "MOBILE", "design")))
                .execute(model);

        assertEquals(List.of(first, second, third), model.getGigList());
        assertEquals(List.of(ALICE), model.getFilteredClientList());
    }

    @Test
    public void execute_noMatch_returnsClearMessage() {
        ModelManager model = new ModelManager();
        CommandResult result = new FindGigCommand(
                new GigTitleContainsKeywordsPredicate(List.of("missing"))).execute(model);

        assertEquals(FindGigCommand.MESSAGE_NO_MATCHES, result.getFeedbackToUser());
    }

    private static Gig gig(gigabyte.model.client.Client client, String title) {
        return new Gig(client, new GigTitle(title), GigStatus.IN_PROGRESS,
                new Deadline("2027-01-01"), new Fee("100"));
    }
}
