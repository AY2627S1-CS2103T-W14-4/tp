package gigabyte.model;

import gigabyte.model.client.Client;
import gigabyte.model.gig.Gig;
import javafx.collections.ObservableList;

/**
 * Unmodifiable view of Gigabyte data.
 */
public interface ReadOnlyGigabyteData {

    /**
     * Returns an unmodifiable view of the clients.
     */
    ObservableList<Client> getClientList();

    /**
     * Returns an unmodifiable view of the gigs.
     */
    ObservableList<Gig> getGigList();
}
