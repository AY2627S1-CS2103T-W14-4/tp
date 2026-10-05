package gigabyte.model;

import gigabyte.model.client.Client;
import javafx.collections.ObservableList;

/**
 * Unmodifiable view of an client list
 */
public interface ReadOnlyGigabyteData {

    /**
     * Returns an unmodifiable view of the clients list.
     * This list will not contain any duplicate clients.
     */
    ObservableList<Client> getClientList();

}
