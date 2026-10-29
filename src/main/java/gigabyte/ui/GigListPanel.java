package gigabyte.ui;

import static java.util.Objects.requireNonNull;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.function.Consumer;

import gigabyte.model.client.Client;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigOrdering;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitleContainsKeywordsPredicate;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

/** Panel displaying gigs for the selected client or a requested status across all clients. */
public class GigListPanel extends UiPart<Region> {
    /** Shared ordering for active deadline views. */
    public static final Comparator<Gig> DEADLINE_COMPARATOR = Comparator
            .comparing((Gig gig) -> gig.getDeadline().getValue())
            .thenComparing(gig -> gig.getClient().getName().toString())
            .thenComparing(gig -> gig.getTitle().toString())
            .thenComparing(Gig::getUid);
    private static final String NO_CLIENT = "Select a client to view their gigs.";
    private static final String NO_GIGS = "This client has no gigs.";
    private static final String NO_DEADLINES = "There are no active gig deadlines.";
    private final FilteredList<Gig> filteredGigs;
    private final Clock clock;
    private final Consumer<Gig> gigSelectionHandler;
    private final PauseTransition deadlineRefreshTimer = new PauseTransition();
    private Gig selectionBeforeUpdate;
    private Gig reportedSelection;
    private boolean isUpdatingGigs;
    private Client selectedClient;
    private GigStatus selectedStatus;
    private boolean isShowingDeadlineOverview;
    private boolean isTitleSearchActive;
    @javafx.fxml.FXML private Label emptyState;
    @javafx.fxml.FXML private ListView<Gig> gigListView;

    /** Creates a gig list panel backed by the given gigs. */
    public GigListPanel(ObservableList<Gig> gigs) {
        this(gigs, gig -> { });
    }

    /** Creates a gig list panel backed by the given gigs and gig-selection handler. */
    public GigListPanel(ObservableList<Gig> gigs, Consumer<Gig> gigSelectionHandler) {
        this(gigs, gigSelectionHandler, Clock.systemDefaultZone());
    }

    GigListPanel(ObservableList<Gig> gigs, Consumer<Gig> gigSelectionHandler, Clock clock) {
        super("GigListPanel.fxml", new BorderPane());
        this.clock = clock;
        this.gigSelectionHandler = gigSelectionHandler;
        // Capture the selected identity before the filtered/sorted lists process the source change.
        gigs.addListener((ListChangeListener<Gig>) change -> {
            selectionBeforeUpdate = gigListView.getSelectionModel().getSelectedItem();
            isUpdatingGigs = true;
        });
        filteredGigs = new FilteredList<>(gigs, gig -> false);
        SortedList<Gig> sortedGigs = new SortedList<>(filteredGigs);
        gigListView.setItems(sortedGigs);
        gigListView.setCellFactory(view -> {
            ListCell<Gig> cell = new ListCell<>() {
                @Override protected void updateItem(Gig gig, boolean empty) {
                    super.updateItem(gig, empty);
                    String overdue = gig != null && gig.getStatus() != GigStatus.COMPLETED
                            && gig.getDeadline().getValue().isBefore(LocalDate.now(clock))
                            ? "    OVERDUE" : "";
                    setText(empty || gig == null ? null : String.format(
                            "%s%nClient: %s%nStatus: %s    Deadline: %s%s    Fee: $%s",
                            gig.getTitle(), gig.getClient().getName(), gig.getStatus(),
                            gig.getDeadline(), overdue, gig.getAgreedFee()));
                    setWrapText(true);
                }
            };
            cell.getStyleClass().add("gig-list-cell");
            cell.prefWidthProperty().bind(view.widthProperty().subtract(20));
            return cell;
        });
        // Row changes detect field-identical gigs; item changes detect a replacement at the same row.
        gigListView.getSelectionModel().selectedIndexProperty().addListener((observable, oldIndex, newIndex) ->
                notifyGigSelection());
        gigListView.getSelectionModel().selectedItemProperty().addListener((observable, oldGig, newGig) ->
                notifyGigSelection());
        // Register after the transformations so restoration sees the final visible order. Intermediate
        // selections made by JavaFX must not switch the payment panel to a neighboring gig.
        gigs.addListener((ListChangeListener<Gig>) change -> {
            restoreGigSelection();
            isUpdatingGigs = false;
            notifyGigSelection();
        });
        // A blank hint gives its height to the list, which matters in short windows.
        emptyState.managedProperty().bind(emptyState.textProperty().isNotEmpty());
        updateEmptyState();
        filteredGigs.addListener((javafx.collections.ListChangeListener<Gig>) change -> updateEmptyState());
        Platform.runLater(this::scheduleDeadlineRefresh);
    }

    private void restoreGigSelection() {
        if (selectionBeforeUpdate != null) {
            for (int index = 0; index < gigListView.getItems().size(); index++) {
                if (gigListView.getItems().get(index).hasSameUid(selectionBeforeUpdate)) {
                    gigListView.getSelectionModel().clearAndSelect(index);
                    return;
                }
            }
        }
        gigListView.getSelectionModel().clearSelection();
    }

    private void notifyGigSelection() {
        if (isUpdatingGigs) {
            return;
        }
        int index = gigListView.getSelectionModel().getSelectedIndex();
        Gig selected = index < 0 ? null : gigListView.getItems().get(index);
        if (selected == null ? reportedSelection != null
                : reportedSelection == null || !selected.hasSameUid(reportedSelection)) {
            reportedSelection = selected;
            gigSelectionHandler.accept(selected);
        }
    }

    /** Displays only the gigs belonging to {@code client}. */
    public void showGigsFor(Client client) {
        gigListView.getSelectionModel().clearSelection();
        selectedClient = client;
        selectedStatus = null;
        isShowingDeadlineOverview = false;
        isTitleSearchActive = false;
        getSortedGigs().setComparator(null);
        filteredGigs.setPredicate(gig -> isGigForClient(gig, client));
        updateEmptyState();
    }

    /**
     * Displays gigs matching {@code status} across all clients in their original order.
     */
    public void showGigsWithStatus(GigStatus status) {
        requireNonNull(status);
        gigListView.getSelectionModel().clearSelection();
        selectedClient = null;
        selectedStatus = status;
        isShowingDeadlineOverview = false;
        isTitleSearchActive = false;
        getSortedGigs().setComparator(null);
        filteredGigs.setPredicate(gig -> gig.getStatus() == status);
        updateEmptyState();
    }

    /** Displays all non-completed gigs in deadline order. */
    public void showDeadlineOverview() {
        gigListView.getSelectionModel().clearSelection();
        isShowingDeadlineOverview = true;
        selectedClient = null;
        selectedStatus = null;
        isTitleSearchActive = false;
        filteredGigs.setPredicate(gig -> gig.getStatus() != GigStatus.COMPLETED);
        getSortedGigs().setComparator(DEADLINE_COMPARATOR);
        updateEmptyState();
    }

    /** Displays all gigs whose titles match any supplied keyword. */
    public void showGigsMatchingTitle(GigTitleContainsKeywordsPredicate predicate) {
        requireNonNull(predicate);
        gigListView.getSelectionModel().clearSelection();
        selectedClient = null;
        selectedStatus = null;
        isShowingDeadlineOverview = false;
        isTitleSearchActive = true;
        getSortedGigs().setComparator(null);
        filteredGigs.setPredicate(predicate);
        updateEmptyState();
    }

    /** Returns whether {@code gig} belongs to {@code client}. */
    static boolean isGigForClient(Gig gig, Client client) {
        return GigOrdering.isForClient(gig, client);
    }

    @SuppressWarnings("unchecked")
    private SortedList<Gig> getSortedGigs() {
        return (SortedList<Gig>) gigListView.getItems();
    }

    /** Refreshes overdue labels after the local date changes. */
    void refreshDeadlineStates() {
        gigListView.refresh();
    }

    private void scheduleDeadlineRefresh() {
        ZonedDateTime currentDateTime = ZonedDateTime.now(clock);
        ZonedDateTime nextMidnight = currentDateTime.toLocalDate().plusDays(1)
                .atStartOfDay(clock.getZone());
        long delayMillis = java.time.Duration.between(currentDateTime, nextMidnight).toMillis();
        deadlineRefreshTimer.setDuration(javafx.util.Duration.millis(Math.max(1, delayMillis)));
        deadlineRefreshTimer.setOnFinished(event -> {
            refreshDeadlineStates();
            scheduleDeadlineRefresh();
        });
        deadlineRefreshTimer.playFromStart();
    }

    /** Returns the gigs currently displayed by this panel. */
    ListView<Gig> getGigListView() {
        return gigListView;
    }

    /** Returns the empty-state label used by this panel. */
    Label getEmptyState() {
        return emptyState;
    }

    private void updateEmptyState() {
        if (selectedStatus != null) {
            emptyState.setText(filteredGigs.isEmpty() ? "No gigs with status " + selectedStatus + "." : "");
            return;
        }
        if (isShowingDeadlineOverview) {
            emptyState.setText(filteredGigs.isEmpty() ? NO_DEADLINES : "");
            return;
        }
        if (isTitleSearchActive) {
            emptyState.setText(filteredGigs.isEmpty() ? "No gigs match the supplied title keywords." : "");
            return;
        }
        emptyState.setText(selectedClient == null ? NO_CLIENT
                : (filteredGigs.isEmpty() ? NO_GIGS : ""));
    }
}
