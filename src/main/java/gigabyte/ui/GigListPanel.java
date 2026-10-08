package gigabyte.ui;

import gigabyte.model.client.Client;
import gigabyte.model.gig.Gig;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

/** Panel displaying gigs for the selected client. */
public class GigListPanel extends UiPart<Region> {
    private static final String NO_CLIENT = "Select a client to view their gigs.";
    private static final String NO_GIGS = "This client has no gigs.";
    private final FilteredList<Gig> filteredGigs;
    private Client selectedClient;
    @javafx.fxml.FXML private Label emptyState;
    @javafx.fxml.FXML private ListView<Gig> gigListView;

    /** Creates a gig list panel backed by the given gigs. */
    public GigListPanel(ObservableList<Gig> gigs) {
        super("GigListPanel.fxml", new BorderPane());
        filteredGigs = new FilteredList<>(gigs, gig -> false);
        gigListView.setItems(filteredGigs);
        gigListView.setCellFactory(view -> new ListCell<>() {
            @Override protected void updateItem(Gig gig, boolean empty) {
                super.updateItem(gig, empty);
                setText(empty || gig == null ? null : String.format(
                        "Status: %s    Deadline: %s    Fee: $%s",
                        gig.getStatus(), gig.getDeadline(), gig.getAgreedFee()));
            }
        });
        updateEmptyState();
        filteredGigs.addListener((javafx.collections.ListChangeListener<Gig>) change -> updateEmptyState());
    }

    /** Displays only the gigs belonging to {@code client}. */
    public void showGigsFor(Client client) {
        selectedClient = client;
        filteredGigs.setPredicate(gig -> isGigForClient(gig, client));
        updateEmptyState();
    }

    /** Returns whether {@code gig} belongs to {@code client}. */
    static boolean isGigForClient(Gig gig, Client client) {
        return client != null && gig.getClient().hasSameUid(client);
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
        emptyState.setText(selectedClient == null ? NO_CLIENT
                : (filteredGigs.isEmpty() ? NO_GIGS : ""));
    }
}
