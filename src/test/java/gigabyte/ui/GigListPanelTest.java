package gigabyte.ui;

import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static gigabyte.testutil.TypicalClients.CARL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.commands.EditGigCommand;
import gigabyte.model.GigabyteData;
import gigabyte.model.ModelManager;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;
import gigabyte.model.gig.GigTitleContainsKeywordsPredicate;
import gigabyte.model.gig.PaymentObligation;
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
    public void showGigsMatchingTitle_filtersAcrossClientsAndTracksAddedOrEditedGigs() {
        ObservableList<Gig> gigs = FXCollections.observableArrayList(ALICE_GIG, BENSON_GIG);
        GigListPanel panel = new GigListPanel(gigs);
        panel.showGigsMatchingTitle(new GigTitleContainsKeywordsPredicate(List.of("design")));

        assertEquals(List.of(BENSON_GIG), panel.getGigListView().getItems());
        Gig added = new Gig(ALICE, new GigTitle("Website design"), GigStatus.NOT_STARTED,
                new Deadline("2027-02-01"), new Fee("300"));
        gigs.add(added);
        assertEquals(List.of(BENSON_GIG, added), panel.getGigListView().getItems());

        gigs.set(1, new Gig(BENSON, new GigTitle("Brand identity"), GigStatus.COMPLETED,
                new Deadline("2027-01-31"), new Fee("200")));
        assertEquals(List.of(added), panel.getGigListView().getItems());
    }

    @Test
    public void showGigsMatchingTitle_noMatch_showsEmptyStateAndSelectionClears() {
        GigListPanel panel = createPanel();
        panel.showGigsFor(ALICE);
        panel.getGigListView().getSelectionModel().select(0);
        panel.showGigsMatchingTitle(new GigTitleContainsKeywordsPredicate(List.of("missing")));

        assertTrue(panel.getGigListView().getItems().isEmpty());
        assertEquals("No gigs match the supplied title keywords.", panel.getEmptyState().getText());
        assertNull(panel.getGigListView().getSelectionModel().getSelectedItem());
    }

    @Test
    public void replacingGigInObservableList_refreshesDisplayedStatusImmediately() {
        GigabyteData data = new GigabyteData();
        data.addClient(ALICE);
        data.addGig(ALICE_GIG);
        GigListPanel panel = new GigListPanel(data.getGigList());
        panel.showGigsFor(ALICE);
        Gig currentGig = data.getGigList().get(0);
        Gig updatedGig = currentGig.withStatus(GigStatus.COMPLETED);

        data.setGig(currentGig, updatedGig);

        assertEquals(updatedGig, panel.getGigListView().getItems().get(0));
        new Scene(panel.getRoot(), 500, 300);
        panel.getRoot().applyCss();
        panel.getRoot().layout();
        @SuppressWarnings("unchecked")
        ListCell<Gig> cell = (ListCell<Gig>) panel.getGigListView().lookup(".list-cell");
        assertTrue(cell.getText().contains("Status: COMPLETED"));
    }

    @Test
    public void editGig_refreshesClientStatusAndDeadlineViewsWithoutClearingSelectedPayments() throws Exception {
        ModelManager model = new ModelManager();
        model.addClient(ALICE);
        model.addClient(BENSON);
        model.addGig(ALICE_GIG);
        model.addGig(ALICE_GIG.withNewUid());
        model.addGig(ALICE_GIG.withClient(BENSON).withNewUid());
        Gig target = model.getGigList().get(1);
        model.addPaymentObligation(new PaymentObligation(target, new Fee("50"), new Deadline("2027-01-31"), false));
        PaymentObligationListPanel payments = new PaymentObligationListPanel(model.getPaymentObligationList());
        GigListPanel clientView = new GigListPanel(model.getGigList(), payments::showPaymentsFor);
        GigListPanel statusView = new GigListPanel(model.getGigList());
        GigListPanel deadlinesView = new GigListPanel(model.getGigList());
        clientView.showGigsFor(ALICE);
        clientView.getGigListView().getSelectionModel().select(1);
        statusView.showGigsWithStatus(GigStatus.IN_PROGRESS);
        deadlinesView.showDeadlineOverview();

        new EditGigCommand(Index.fromOneBased(1), Index.fromOneBased(2),
                Optional.of(new GigTitle("Edited title")),
                Optional.of(new Deadline("2026-01-01")),
                Optional.of(new Fee("1800"))).execute(model);

        Gig edited = model.getGigList().get(1);
        assertEquals(edited, clientView.getGigListView().getSelectionModel().getSelectedItem());
        assertEquals(edited.getUid(), payments.getPaymentListView().getItems().get(0).getGig().getUid());
        assertEquals(edited, statusView.getGigListView().getItems().get(1));
        assertEquals(edited.getUid(), deadlinesView.getGigListView().getItems().get(0).getUid());
        for (GigListPanel view : List.of(clientView, statusView, deadlinesView)) {
            new Scene(view.getRoot(), 500, 300);
            view.getRoot().applyCss();
            view.getRoot().layout();
            assertTrue(view.getGigListView().lookupAll(".list-cell").stream().anyMatch(node ->
                    node instanceof ListCell<?> cell && !cell.isEmpty()
                    && cell.getText().contains("Edited title") && cell.getText().contains("1800.00")
                    && cell.getText().contains("2026-01-01")));
        }
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

        assertEquals(List.of(overdue, ALICE_GIG, sameDate), panel.getGigListView().getItems());
        assertEquals("", panel.getEmptyState().getText());
    }

    @Test
    public void refreshDeadlineStates_localDateAdvances_updatesActiveGigToOverdue() {
        MutableClock mutableClock = new MutableClock(Instant.parse("2026-10-11T04:00:00Z"),
                ZoneId.of("Asia/Kuala_Lumpur"));
        Gig dueToday = new Gig(ALICE, new GigTitle("Due today"), GigStatus.IN_PROGRESS,
                new Deadline("2026-10-11"), new Fee("100"));
        GigListPanel panel = new GigListPanel(
                FXCollections.observableArrayList(dueToday), gig -> { }, mutableClock);
        panel.showDeadlineOverview();
        assertFalse(renderCells(panel).get(0).getText().contains("OVERDUE"));

        mutableClock.advance(Duration.ofDays(1));
        panel.refreshDeadlineStates();

        assertTrue(renderCells(panel).get(0).getText().contains("OVERDUE"));
    }

    @Test
    public void gigCell_completedPastDeadline_notMarkedOverdue() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-11T04:00:00Z"),
                ZoneId.of("Asia/Kuala_Lumpur"));
        Gig completed = new Gig(ALICE, new GigTitle("Completed work"), GigStatus.COMPLETED,
                new Deadline("2026-10-10"), new Fee("100"));
        GigListPanel panel = new GigListPanel(
                FXCollections.observableArrayList(completed), gig -> { }, clock);
        panel.showGigsFor(ALICE);

        assertFalse(renderCells(panel).get(0).getText().contains("OVERDUE"));
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
    public void gigCell_renderedWithDarkTheme_usesReadableTextColor() {
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

    @SuppressWarnings("unchecked")
    private List<ListCell<Gig>> renderCells(GigListPanel panel) {
        if (panel.getRoot().getScene() == null) {
            new Scene(panel.getRoot(), 500, 300);
        }
        panel.getRoot().applyCss();
        panel.getRoot().layout();
        return panel.getGigListView().lookupAll(".list-cell").stream()
                .map(node -> (ListCell<Gig>) node)
                .filter(cell -> !cell.isEmpty())
                .sorted(Comparator.comparingInt((ListCell<Gig> cell) -> cell.getIndex()))
                .toList();
    }

    private static class MutableClock extends Clock {
        private Instant currentInstant;
        private final ZoneId zoneId;

        MutableClock(Instant currentInstant, ZoneId zoneId) {
            this.currentInstant = currentInstant;
            this.zoneId = zoneId;
        }

        void advance(Duration duration) {
            currentInstant = currentInstant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zoneId;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(currentInstant, zone);
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }
    }
}
