package gigabyte.ui;

import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import javafx.application.Platform;
import javafx.collections.FXCollections;

public class GigListPanelTest {
    private static final Gig ALICE_GIG = new Gig(ALICE, GigStatus.IN_PROGRESS,
            new Deadline("2026-12-31"), new Fee("100"));
    private static final Gig BENSON_GIG = new Gig(BENSON, GigStatus.COMPLETED,
            new Deadline("2027-01-31"), new Fee("200"));

    @BeforeAll
    public static void setUpJavaFx() throws Exception {
        Platform.startup(() -> { });
    }

    @Test
    public void showGigsFor_selectedClient_showsOnlyMatchingGigs() {
        GigListPanel panel = createPanel();

        panel.showGigsFor(ALICE);

        assertEquals(1, panel.gigListView.getItems().size());
        assertEquals(ALICE_GIG, panel.gigListView.getItems().get(0));
    }

    @Test
    public void showGigsFor_clientWithoutGigs_showsEmptyState() {
        GigListPanel panel = createPanel();

        panel.showGigsFor(new gigabyte.testutil.ClientBuilder().withName("No Gigs").build());

        assertEquals(0, panel.gigListView.getItems().size());
        assertEquals("This client has no gigs.", panel.emptyState.getText());
    }

    private GigListPanel createPanel() {
        return new GigListPanel(FXCollections.observableArrayList(ALICE_GIG, BENSON_GIG));
    }
}
