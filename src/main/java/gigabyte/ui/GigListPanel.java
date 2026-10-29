package gigabyte.ui;

import static java.util.Objects.requireNonNull;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
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
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

/** Panel displaying gigs for a client, by status or title, or in deadline order. */
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
    private final SortedList<Gig> sortedGigs;
    private final Clock clock;
    private final Consumer<Gig> gigSelectionHandler;
    private final PauseTransition deadlineRefreshTimer = new PauseTransition();

    private UUID selectedGigIdBeforeUpdate;
    private UUID lastNotifiedGigId;
    private boolean isProcessingSourceChange;
    private ViewMode viewMode = ViewMode.CLIENT;
    private Client selectedClient;
    private GigStatus selectedStatus;

    @FXML
    private Label emptyState;
    @FXML
    private ListView<Gig> gigListView;

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

        // Listener registration must bracket the transformations to preserve the selected gig's identity.
        captureSelectionBeforeSourceChanges(gigs);
        filteredGigs = new FilteredList<>(gigs, gig -> false);
        sortedGigs = new SortedList<>(filteredGigs);
        configureListView();
        configureSelectionListeners();
        restoreSelectionAfterSourceChanges(gigs);

        configureEmptyState();
        Platform.runLater(this::scheduleDeadlineRefresh);
    }

    private void captureSelectionBeforeSourceChanges(ObservableList<Gig> gigs) {
        gigs.addListener((ListChangeListener<Gig>) change -> {
            Gig selected = gigListView.getSelectionModel().getSelectedItem();
            selectedGigIdBeforeUpdate = selected == null ? null : selected.getUid();
            isProcessingSourceChange = true;
        });
    }

    private void configureListView() {
        gigListView.setItems(sortedGigs);
        gigListView.setCellFactory(view -> createGigCell());
    }

    private ListCell<Gig> createGigCell() {
        ListCell<Gig> cell = new ListCell<>() {
            @Override
            protected void updateItem(Gig gig, boolean empty) {
                super.updateItem(gig, empty);
                if (empty || gig == null) {
                    setText(null);
                    return;
                }

                String overdue = isOverdue(gig) ? "    OVERDUE" : "";
                setText(String.format("%s%nClient: %s%nStatus: %s    Deadline: %s%s    Fee: %s",
                        gig.getTitle(), gig.getClient().getName(), gig.getStatus(),
                        gig.getDeadline(), overdue, gig.getAgreedFee().toDisplayString()));
            }
        };
        cell.setWrapText(true);
        cell.getStyleClass().add("gig-list-cell");
        cell.prefWidthProperty().bind(gigListView.widthProperty().subtract(20));
        return cell;
    }

    private boolean isOverdue(Gig gig) {
        return gig.getStatus() != GigStatus.COMPLETED
                && gig.getDeadline().getValue().isBefore(LocalDate.now(clock));
    }

    private void configureSelectionListeners() {
        // Row changes detect field-identical gigs; item changes detect a replacement at the same row.
        gigListView.getSelectionModel().selectedIndexProperty().addListener((observable, oldIndex, newIndex) ->
                notifyGigSelection());
        gigListView.getSelectionModel().selectedItemProperty().addListener((observable, oldGig, newGig) ->
                notifyGigSelection());
    }

    private void restoreSelectionAfterSourceChanges(ObservableList<Gig> gigs) {
        // Register after the transformations so restoration sees the final visible order. Intermediate
        // selections made by JavaFX must not switch the payment panel to a neighboring gig.
        gigs.addListener((ListChangeListener<Gig>) change -> {
            restoreGigSelection();
            isProcessingSourceChange = false;
            notifyGigSelection();
        });
    }

    private void configureEmptyState() {
        // A blank hint gives its height to the list, which matters in short windows.
        emptyState.managedProperty().bind(emptyState.textProperty().isNotEmpty());
        updateEmptyState();
        filteredGigs.addListener((ListChangeListener<Gig>) change -> updateEmptyState());
    }

    private void restoreGigSelection() {
        if (selectedGigIdBeforeUpdate != null) {
            for (int index = 0; index < sortedGigs.size(); index++) {
                if (sortedGigs.get(index).getUid().equals(selectedGigIdBeforeUpdate)) {
                    gigListView.getSelectionModel().clearAndSelect(index);
                    return;
                }
            }
        }
        gigListView.getSelectionModel().clearSelection();
    }

    private void notifyGigSelection() {
        if (isProcessingSourceChange) {
            return;
        }
        int index = gigListView.getSelectionModel().getSelectedIndex();
        Gig selected = index < 0 ? null : sortedGigs.get(index);
        UUID selectedGigId = selected == null ? null : selected.getUid();
        if (Objects.equals(selectedGigId, lastNotifiedGigId)) {
            return;
        }
        lastNotifiedGigId = selectedGigId;
        gigSelectionHandler.accept(selected);
    }

    /** Displays only the gigs belonging to {@code client}. */
    public void showGigsFor(Client client) {
        gigListView.getSelectionModel().clearSelection();
        viewMode = ViewMode.CLIENT;
        selectedClient = client;
        selectedStatus = null;
        sortedGigs.setComparator(null);
        filteredGigs.setPredicate(gig -> isGigForClient(gig, client));
        updateEmptyState();
    }

    /**
     * Displays gigs matching {@code status} across all clients in their original order.
     */
    public void showGigsWithStatus(GigStatus status) {
        requireNonNull(status);
        gigListView.getSelectionModel().clearSelection();
        viewMode = ViewMode.STATUS;
        selectedClient = null;
        selectedStatus = status;
        sortedGigs.setComparator(null);
        filteredGigs.setPredicate(gig -> gig.getStatus() == status);
        updateEmptyState();
    }

    /** Displays all non-completed gigs in deadline order. */
    public void showDeadlineOverview() {
        gigListView.getSelectionModel().clearSelection();
        viewMode = ViewMode.DEADLINES;
        selectedClient = null;
        selectedStatus = null;
        filteredGigs.setPredicate(gig -> gig.getStatus() != GigStatus.COMPLETED);
        sortedGigs.setComparator(DEADLINE_COMPARATOR);
        updateEmptyState();
    }

    /** Displays all gigs whose titles match any supplied keyword. */
    public void showGigsMatchingTitle(GigTitleContainsKeywordsPredicate predicate) {
        requireNonNull(predicate);
        gigListView.getSelectionModel().clearSelection();
        viewMode = ViewMode.TITLE_SEARCH;
        selectedClient = null;
        selectedStatus = null;
        sortedGigs.setComparator(null);
        filteredGigs.setPredicate(predicate);
        updateEmptyState();
    }

    /** Returns whether {@code gig} belongs to {@code client}. */
    static boolean isGigForClient(Gig gig, Client client) {
        return GigOrdering.isForClient(gig, client);
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
        if (!filteredGigs.isEmpty()) {
            emptyState.setText("");
            return;
        }
        emptyState.setText(switch (viewMode) {
            case CLIENT -> selectedClient == null ? NO_CLIENT : NO_GIGS;
            case STATUS -> "No gigs with status " + selectedStatus + ".";
            case DEADLINES -> NO_DEADLINES;
            case TITLE_SEARCH -> "No gigs match the supplied title keywords.";
        });
    }

    /** The mutually exclusive gig views supported by this panel. */
    private enum ViewMode {
        CLIENT, STATUS, DEADLINES, TITLE_SEARCH
    }
}
