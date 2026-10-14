package gigabyte.ui;

import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static gigabyte.testutil.TypicalClients.CARL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollBar;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

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
    public void selectingGig_selectedGig_invokesSelectionHandler() {
        AtomicReference<Gig> selectedGig = new AtomicReference<>();
        GigListPanel panel = new GigListPanel(
                FXCollections.observableArrayList(ALICE_GIG, BENSON_GIG), selectedGig::set);
        panel.showGigsFor(ALICE);

        panel.getGigListView().getSelectionModel().select(0);

        assertEquals(ALICE_GIG, selectedGig.get());
    }

    @Test
    public void showGigsFor_anotherClient_clearsPreviousGigSelection() {
        AtomicReference<Gig> selectedGig = new AtomicReference<>();
        GigListPanel panel = new GigListPanel(
                FXCollections.observableArrayList(ALICE_GIG, BENSON_GIG), selectedGig::set);
        panel.showGigsFor(ALICE);
        panel.getGigListView().getSelectionModel().select(0);

        panel.showGigsFor(BENSON);

        assertNull(panel.getGigListView().getSelectionModel().getSelectedItem());
        assertNull(selectedGig.get());
    }

    @Test
    public void showGigsWithStatus_multipleClients_filtersInOriginalOrderAndTracksNewGigs() {
        Gig matchingGig = new Gig(BENSON, new GigTitle("Branding"), GigStatus.IN_PROGRESS,
                new Deadline("2027-02-28"), new Fee("300"));
        ObservableList<Gig> gigs = FXCollections.observableArrayList(ALICE_GIG, BENSON_GIG, matchingGig);
        GigListPanel panel = new GigListPanel(gigs);

        panel.showGigsWithStatus(GigStatus.IN_PROGRESS);
        assertEquals(List.of(ALICE_GIG, matchingGig), panel.getGigListView().getItems());
        assertEquals("", panel.getEmptyState().getText());

        gigs.remove(ALICE_GIG);
        assertEquals(List.of(matchingGig), panel.getGigListView().getItems());
        gigs.remove(matchingGig);
        assertEquals("No gigs with status IN_PROGRESS.", panel.getEmptyState().getText());
        gigs.add(ALICE_GIG);
        assertEquals(List.of(ALICE_GIG), panel.getGigListView().getItems());
        assertEquals("", panel.getEmptyState().getText());
    }

    @Test
    public void showGigsWithStatus_previousSelection_clearsSelectionAndAllowsReturnToClientView() {
        GigListPanel panel = createPanel();
        panel.showGigsFor(ALICE);
        panel.getGigListView().getSelectionModel().select(0);

        panel.showGigsWithStatus(GigStatus.NOT_STARTED);
        assertNull(panel.getGigListView().getSelectionModel().getSelectedItem());
        assertEquals(0, panel.getGigListView().getItems().size());
        assertEquals("No gigs with status NOT_STARTED.", panel.getEmptyState().getText());

        panel.showGigsFor(ALICE);
        assertEquals(List.of(ALICE_GIG), panel.getGigListView().getItems());
        panel.showGigsWithStatus(GigStatus.COMPLETED);
        assertEquals(List.of(BENSON_GIG), panel.getGigListView().getItems());
        panel.showGigsFor(null);
        assertEquals("Select a client to view their gigs.", panel.getEmptyState().getText());
    }

    @Test
    public void showDeadlineOverview_filtersCompletedAndSortsAcrossClients() {
        Gig overdue = new Gig(BENSON, new GigTitle("Earlier"), GigStatus.IN_PROGRESS,
                new Deadline("2020-01-01"), new Fee("50"));
        Gig sameDate = new Gig(ALICE, new GigTitle("Later"), GigStatus.NOT_STARTED,
                new Deadline("2027-01-31"), new Fee("60"));
        ObservableList<Gig> gigs = FXCollections.observableArrayList(
                sameDate, BENSON_GIG, ALICE_GIG, overdue);
        GigListPanel panel = new GigListPanel(gigs);

        panel.showDeadlineOverview();
        panel.refreshDeadlineStates();

        assertEquals(List.of(overdue, ALICE_GIG, sameDate), panel.getGigListView().getItems());
        assertEquals("", panel.getEmptyState().getText());
    }

    @Test
    public void showDeadlineOverview_tracksChangesAndShowsEmptyState() {
        ObservableList<Gig> gigs = FXCollections.observableArrayList(ALICE_GIG, BENSON_GIG);
        GigListPanel panel = new GigListPanel(gigs);
        panel.showDeadlineOverview();

        gigs.remove(ALICE_GIG);
        assertEquals("There are no active gig deadlines.", panel.getEmptyState().getText());
        Gig added = new Gig(CARL, new GigTitle("New work"), GigStatus.NOT_STARTED,
                new Deadline("2026-11-01"), new Fee("70"));
        gigs.add(added);
        assertEquals(List.of(added), panel.getGigListView().getItems());
    }

    @Test
    public void showDeadlineOverview_thenSelectClient_returnsClientView() {
        GigListPanel panel = createPanel();
        panel.showDeadlineOverview();
        panel.showGigsFor(ALICE);

        assertEquals(List.of(ALICE_GIG), panel.getGigListView().getItems());
    }

    @Test
    public void showGigsWithStatus_manyMatches_keepsCompleteDetailsAndVerticalScrolling() throws Exception {
        FutureTask<Void> test = new FutureTask<>(() -> {
            ObservableList<Gig> gigs = FXCollections.observableArrayList();
            for (int i = 0; i < 30; i++) {
                gigs.add(new Gig(i % 2 == 0 ? ALICE : BENSON, new GigTitle("Website " + i),
                        GigStatus.IN_PROGRESS, new Deadline("2027-01-31"), new Fee("100")));
            }
            GigListPanel panel = new GigListPanel(gigs);
            panel.showGigsWithStatus(GigStatus.IN_PROGRESS);
            Stage stage = new Stage();
            Scene scene = new Scene(panel.getRoot(), 340, 300);
            scene.getStylesheets().add(getClass().getResource("/view/DarkTheme.css").toExternalForm());
            stage.setScene(scene);
            try {
                stage.show();
                panel.getRoot().applyCss();
                panel.getRoot().layout();

                assertEquals(gigs, panel.getGigListView().getItems());
                @SuppressWarnings("unchecked")
                ListCell<Gig> cell = (ListCell<Gig>) panel.getGigListView().lookup(".list-cell");
                assertTrue(cell.getText().contains("Client: " + cell.getItem().getClient().getName()));
                assertTrue(cell.isWrapText());
                assertTrue(cell.getWidth() <= panel.getGigListView().getWidth());
                var scrollBars = panel.getGigListView().lookupAll(".scroll-bar");
                assertFalse(scrollBars.stream().anyMatch(node -> node instanceof ScrollBar bar
                        && bar.getOrientation() == Orientation.HORIZONTAL && bar.isVisible()));
                assertTrue(scrollBars.stream().anyMatch(node -> node instanceof ScrollBar bar
                        && bar.getOrientation() == Orientation.VERTICAL && bar.isVisible()));
            } finally {
                stage.hide();
            }
            return null;
        });
        Platform.runLater(test);
        test.get(10, TimeUnit.SECONDS);
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
        assertTrue(text.contains("Client: " + ALICE_GIG.getClient().getName()));
        assertTrue(text.contains("Deadline: " + ALICE_GIG.getDeadline()));
        assertTrue(text.contains("Fee: $" + ALICE_GIG.getAgreedFee()));
        assertTrue(cell.isWrapText());
    }

    @Test
    public void gigCell_renderedWithDarkTheme_usesReadableTextColour() {
        GigListPanel panel = createPanel();
        panel.showGigsFor(ALICE);
        Scene scene = new Scene(panel.getRoot(), 500, 300);
        scene.getStylesheets().add(getClass().getResource("/view/DarkTheme.css").toExternalForm());
        panel.getRoot().applyCss();
        panel.getRoot().layout();

        @SuppressWarnings("unchecked")
        ListCell<Gig> cell = (ListCell<Gig>) panel.getGigListView().lookup(".list-cell");

        assertEquals(Color.WHITE, cell.getTextFill());
    }

    @Test
    public void gigCell_narrowPanelWith100CharacterTitle_wrapsWithoutHorizontalOverflow() {
        Gig longTitleGig = new Gig(ALICE, new GigTitle("W".repeat(100)), GigStatus.IN_PROGRESS,
                new Deadline("2026-12-31"), new Fee("100"));
        GigListPanel panel = new GigListPanel(FXCollections.observableArrayList(longTitleGig));
        panel.showGigsFor(ALICE);
        new Scene(panel.getRoot(), 340, 300);
        panel.getRoot().applyCss();
        panel.getRoot().layout();

        @SuppressWarnings("unchecked")
        ListCell<Gig> cell = (ListCell<Gig>) panel.getGigListView().lookup(".list-cell");
        ScrollBar horizontalScrollBar = (ScrollBar) panel.getGigListView().lookup(".scroll-bar:horizontal");

        assertTrue(cell.getWidth() <= panel.getGigListView().getWidth());
        assertTrue(cell.getHeight() > 50);
        assertFalse(horizontalScrollBar.isVisible());
    }

    private GigListPanel createPanel() {
        return new GigListPanel(FXCollections.observableArrayList(ALICE_GIG, BENSON_GIG));
    }
}
