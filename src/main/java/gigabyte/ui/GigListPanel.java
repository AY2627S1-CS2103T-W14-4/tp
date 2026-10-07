package gigabyte.ui;

import gigabyte.model.client.Client;
import gigabyte.model.gig.Gig;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;

/** Displays gigs for the selected client. */
public class GigListPanel extends UiPart<Region> {
    private static final String NO_CLIENT = "Select a client to view their gigs.";
    private static final String NO_GIGS = "This client has no gigs.";
    private final FilteredList<Gig> filteredGigs;
    @javafx.fxml.FXML private Label emptyState;
    @javafx.fxml.FXML private ListView<Gig> gigListView;

    public GigListPanel(ObservableList<Gig> gigs) {
        super("GigListPanel.fxml");
        filteredGigs = new FilteredList<>(gigs);
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

    public void showGigsFor(Client client) {
        filteredGigs.setPredicate(gig -> client != null && gig.getClient().isSameClient(client));
        updateEmptyState();
    }

    private void updateEmptyState() {
        emptyState.setText(filteredGigs.getPredicate() == null ? NO_CLIENT
                : (filteredGigs.isEmpty() ? NO_GIGS : ""));
    }
}
