package gigabyte.ui;

import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static gigabyte.testutil.TypicalClients.CARL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;

public class GigListPanelTest {
    private static final Gig ALICE_GIG = new Gig(ALICE, new GigTitle("Website redesign"), GigStatus.IN_PROGRESS,
            new Deadline("2026-12-31"), new Fee("100"));
    private static final Gig BENSON_GIG = new Gig(BENSON, new GigTitle("Logo design"), GigStatus.COMPLETED,
            new Deadline("2027-01-31"), new Fee("200"));

    @BeforeAll
    public static void setUpJavaFx() {
        JavaFxTestUtil.startToolkit();
    }

    @Test
    public void constructor_startsWithNoSelectedClient() {
        GigListPanel panel = createPanel();

        assertEquals(0, panel.getGigListView().getItems().size());
        assertEquals("Select a client to view their gigs.", panel.getEmptyState().getText());
    }

    @Test
    public void showGigsFor_selectedClient_showsOnlyMatchingGigs() {
        GigListPanel panel = createPanel();

        panel.showGigsFor(ALICE);

        assertEquals(1, panel.getGigListView().getItems().size());
        assertEquals(ALICE_GIG, panel.getGigListView().getItems().get(0));
    }

    @Test
    public void showGigsFor_clientWithoutGigs_showsEmptyState() {
        GigListPanel panel = createPanel();

        panel.showGigsFor(CARL);

        assertEquals("This client has no gigs.", panel.getEmptyState().getText());
    }

    @Test
    public void showGigsFor_nullClient_showsNoClientState() {
        GigListPanel panel = createPanel();

        panel.showGigsFor(null);

        assertEquals("Select a client to view their gigs.", panel.getEmptyState().getText());
    }

    @Test
    public void gigCell_renderedItem_showsTitleFirstAndWrapsCompleteSummary() {
        GigListPanel panel = createPanel();
        panel.showGigsFor(ALICE);
        new Scene(panel.getRoot(), 500, 300);
        panel.getRoot().applyCss();
        panel.getRoot().layout();

        @SuppressWarnings("unchecked")
        ListCell<Gig> cell = (ListCell<Gig>) panel.getGigListView().lookup(".list-cell");
        String text = cell.getText();

        assertTrue(text.startsWith(ALICE_GIG.getTitle().toString()));
        assertTrue(text.contains("Status: " + ALICE_GIG.getStatus()));
        assertTrue(text.contains("Deadline: " + ALICE_GIG.getDeadline()));
        assertTrue(text.contains("Fee: $" + ALICE_GIG.getAgreedFee()));
        assertTrue(cell.isWrapText());
    }

    private GigListPanel createPanel() {
        return new GigListPanel(FXCollections.observableArrayList(ALICE_GIG, BENSON_GIG));
    }
}
