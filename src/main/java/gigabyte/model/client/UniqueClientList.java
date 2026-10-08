package gigabyte.model.client;

import static gigabyte.commons.util.CollectionUtil.requireAllNonNull;
import static java.util.Objects.requireNonNull;

import java.util.Iterator;
import java.util.List;

import gigabyte.model.client.exceptions.ClientNotFoundException;
import gigabyte.model.client.exceptions.DuplicateClientException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * A list of clients that enforces unique IDs and names between its elements and does not allow nulls.
 * {@link #contains(Client)} checks the name constraint; relationships should use stable IDs.
 *
 * Supports a minimal set of list operations.
 *
 * @see Client#isSameClient(Client)
 */
public class UniqueClientList implements Iterable<Client> {

    private final ObservableList<Client> internalList = FXCollections.observableArrayList();
    private final ObservableList<Client> internalUnmodifiableList =
            FXCollections.unmodifiableObservableList(internalList);

    /**
     * Returns true if the list contains a client with the same name as the given argument.
     */
    public boolean contains(Client toCheck) {
        requireNonNull(toCheck);
        return internalList.stream().anyMatch(toCheck::hasSameName);
    }

    /** Returns whether a client with the same stable ID is present. */
    public boolean containsUid(Client toCheck) {
        requireNonNull(toCheck);
        return internalList.stream().anyMatch(toCheck::hasSameUid);
    }

    /**
     * Adds a client to the list.
     * The client must not already exist in the list.
     */
    public void add(Client toAdd) {
        requireNonNull(toAdd);
        if (contains(toAdd) || containsUid(toAdd)) {
            throw new DuplicateClientException();
        }
        internalList.add(toAdd);
    }

    /**
     * Replaces the client {@code target} in the list with {@code editedClient}.
     * {@code target} must exist in the list.
     * The client identity of {@code editedClient} must not be the same as another existing client in the list.
     */
    public void setClient(Client target, Client editedClient) {
        requireAllNonNull(target, editedClient);
        if (!target.hasSameUid(editedClient)) {
            throw new IllegalArgumentException("An edited client must retain its uid.");
        }

        int index = indexOfUid(target);
        if (index == -1) {
            throw new ClientNotFoundException();
        }

        for (int otherIndex = 0; otherIndex < internalList.size(); otherIndex++) {
            if (otherIndex != index && (editedClient.hasSameName(internalList.get(otherIndex))
                    || editedClient.hasSameUid(internalList.get(otherIndex)))) {
                throw new DuplicateClientException();
            }
        }

        internalList.set(index, editedClient);
    }

    /**
     * Removes the equivalent client from the list.
     * The client must exist in the list.
     */
    public void remove(Client toRemove) {
        requireNonNull(toRemove);
        int index = indexOfUid(toRemove);
        if (index < 0) {
            throw new ClientNotFoundException();
        }
        internalList.remove(index);
    }

    public void setClients(UniqueClientList replacement) {
        requireNonNull(replacement);
        internalList.setAll(replacement.internalList);
    }

    /**
     * Replaces the contents of this list with {@code clients}.
     * {@code clients} must not contain duplicate clients.
     */
    public void setClients(List<Client> clients) {
        requireAllNonNull(clients);
        if (!clientsAreUnique(clients)) {
            throw new DuplicateClientException();
        }

        internalList.setAll(clients);
    }

    /**
     * Returns the backing list as an unmodifiable {@code ObservableList}.
     */
    public ObservableList<Client> asUnmodifiableObservableList() {
        return internalUnmodifiableList;
    }

    @Override
    public Iterator<Client> iterator() {
        return internalList.iterator();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof UniqueClientList otherUniqueClientList)) {
            return false;
        }

        return internalList.equals(otherUniqueClientList.internalList);
    }

    @Override
    public int hashCode() {
        return internalList.hashCode();
    }

    @Override
    public String toString() {
        return internalList.toString();
    }

    /**
     * Returns true if {@code clients} contains only unique clients.
     */
    private boolean clientsAreUnique(List<Client> clients) {
        for (int i = 0; i < clients.size() - 1; i++) {
            for (int j = i + 1; j < clients.size(); j++) {
                if (clients.get(i).hasSameName(clients.get(j)) || clients.get(i).hasSameUid(clients.get(j))) {
                    return false;
                }
            }
        }
        return true;
    }

    private int indexOfUid(Client target) {
        for (int index = 0; index < internalList.size(); index++) {
            if (internalList.get(index).hasSameUid(target)) {
                return index;
            }
        }
        return -1;
    }
}
