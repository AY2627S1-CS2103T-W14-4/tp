package gigabyte.model;

import static java.util.Objects.requireNonNull;

import java.util.List;

import gigabyte.commons.util.ToStringBuilder;
import gigabyte.model.client.Client;
import gigabyte.model.client.UniqueClientList;
import javafx.collections.ObservableList;

/**
 * Wraps all data at the address-book level.
 * Duplicates are not allowed (by .isSameClient comparison).
 */
public class GigabyteData implements ReadOnlyGigabyteData {

    private final UniqueClientList clients = new UniqueClientList();

    public GigabyteData() {}

    /**
     * Creates an GigabyteData using the Clients in the {@code toBeCopied}
     */
    public GigabyteData(ReadOnlyGigabyteData toBeCopied) {
        this();
        resetData(toBeCopied);
    }

    //// list overwrite operations

    /**
     * Replaces the contents of the client list with {@code clients}.
     * {@code clients} must not contain duplicate clients.
     */
    public void setClients(List<Client> clients) {
        this.clients.setClients(clients);
    }

    /**
     * Resets the existing data of this {@code GigabyteData} with {@code newData}.
     */
    public void resetData(ReadOnlyGigabyteData newData) {
        requireNonNull(newData);

        setClients(newData.getClientList());
    }

    //// client-level operations

    /**
     * Returns true if a client with the same identity as {@code client} exists in the client list.
     */
    public boolean hasClient(Client client) {
        requireNonNull(client);
        return clients.contains(client);
    }

    /**
     * Adds a client to the client list.
     * The client must not already exist in the client list.
     */
    public void addClient(Client p) {
        clients.add(p);
    }

    /**
     * Replaces the given client {@code target} in the list with {@code editedClient}.
     * {@code target} must exist in the client list.
     * The client identity of {@code editedClient} must not be the same as another existing client in the client list.
     */
    public void setClient(Client target, Client editedClient) {
        requireNonNull(editedClient);

        clients.setClient(target, editedClient);
    }

    /**
     * Removes {@code key} from this {@code GigabyteData}.
     * {@code key} must exist in the client list.
     */
    public void removeClient(Client key) {
        clients.remove(key);
    }

    //// util methods

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("clients", clients)
                .toString();
    }

    @Override
    public ObservableList<Client> getClientList() {
        return clients.asUnmodifiableObservableList();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof GigabyteData otherGigabyteData)) {
            return false;
        }

        return clients.equals(otherGigabyteData.clients);
    }

    @Override
    public int hashCode() {
        return clients.hashCode();
    }
}
