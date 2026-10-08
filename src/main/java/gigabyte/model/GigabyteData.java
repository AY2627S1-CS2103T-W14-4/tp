package gigabyte.model;

import static gigabyte.commons.util.CollectionUtil.requireAllNonNull;
import static java.util.Objects.requireNonNull;

import java.util.List;

import gigabyte.commons.util.ToStringBuilder;
import gigabyte.model.client.Client;
import gigabyte.model.client.UniqueClientList;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.exceptions.ClientHasGigsException;
import gigabyte.model.gig.exceptions.GigClientNotFoundException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Wraps all data at the application level.
 */
public class GigabyteData implements ReadOnlyGigabyteData {

    private final UniqueClientList clients = new UniqueClientList();
    private final ObservableList<Gig> gigs = FXCollections.observableArrayList();
    private final ObservableList<Gig> unmodifiableGigs =
            FXCollections.unmodifiableObservableList(gigs);

    public GigabyteData() {}

    /**
     * Creates a {@code GigabyteData} using the data in {@code toBeCopied}.
     */
    public GigabyteData(ReadOnlyGigabyteData toBeCopied) {
        this();
        resetData(toBeCopied);
    }

    //// list overwrite operations

    /**
     * Replaces the contents of the client list.
     *
     * @throws GigClientNotFoundException if the replacement removes a client
     *         that is still referenced by a gig
     */
    public void setClients(List<Client> clients) {
        requireAllNonNull(clients);

        List<Client> replacementClients = List.copyOf(clients);
        List<Gig> relinkedGigs = linkGigsToClients(replacementClients, gigs);

        this.clients.setClients(replacementClients);
        this.gigs.setAll(relinkedGigs);
    }

    /**
     * Replaces the contents of the gig list.
     *
     * @throws GigClientNotFoundException if a gig refers to a client that does
     *         not exist
     */
    public void setGigs(List<Gig> gigs) {
        requireAllNonNull(gigs);
        this.gigs.setAll(linkGigsToClients(getClientList(), gigs));
    }

    /**
     * Resets this data using {@code newData}.
     */
    public void resetData(ReadOnlyGigabyteData newData) {
        requireNonNull(newData);

        List<Client> replacementClients = List.copyOf(newData.getClientList());
        List<Gig> replacementGigs =
                linkGigsToClients(replacementClients, newData.getGigList());

        clients.setClients(replacementClients);
        gigs.setAll(replacementGigs);
    }

    //// client-level operations

    /**
     * Returns whether an equivalent client exists.
     */
    public boolean hasClient(Client client) {
        requireNonNull(client);
        return clients.contains(client);
    }

    /**
     * Adds a client.
     */
    public void addClient(Client client) {
        clients.add(client);
    }

    /**
     * Replaces {@code target} with {@code editedClient} and updates its gigs.
     */
    public void setClient(Client target, Client editedClient) {
        requireAllNonNull(target, editedClient);

        clients.setClient(target, editedClient);

        for (int index = 0; index < gigs.size(); index++) {
            Gig gig = gigs.get(index);
            if (gig.getClient().isSameClient(target)) {
                gigs.set(index, gig.withClient(editedClient));
            }
        }
    }

    /**
     * Removes {@code key}.
     *
     * @throws ClientHasGigsException if a gig still refers to the client
     */
    public void removeClient(Client key) {
        requireNonNull(key);

        boolean hasAssociatedGig = gigs.stream()
                .anyMatch(gig -> gig.getClient().isSameClient(key));

        if (hasAssociatedGig) {
            throw new ClientHasGigsException();
        }

        clients.remove(key);
    }

    //// gig-level operations

    /**
     * Adds a gig, linking it to the canonical client stored in this data.
     *
     * @throws GigClientNotFoundException if the gig's client does not exist
     */
    public void addGig(Gig gig) {
        requireNonNull(gig);

        Client storedClient = findMatchingClient(getClientList(), gig.getClient());
        gigs.add(gig.withClient(storedClient));
    }

    //// accessors

    @Override
    public ObservableList<Client> getClientList() {
        return clients.asUnmodifiableObservableList();
    }

    @Override
    public ObservableList<Gig> getGigList() {
        return unmodifiableGigs;
    }

    private static List<Gig> linkGigsToClients(
            List<Client> clients, List<Gig> gigs) {
        requireAllNonNull(clients);
        requireAllNonNull(gigs);

        return gigs.stream()
                .map(gig -> gig.withClient(
                        findMatchingClient(clients, gig.getClient())))
                .toList();
    }

    private static Client findMatchingClient(
            List<Client> clients, Client client) {
        requireNonNull(client);

        return clients.stream()
                .filter(storedClient -> storedClient.isSameClient(client))
                .findFirst()
                .orElseThrow(GigClientNotFoundException::new);
    }

    //// util methods

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("clients", clients)
                .add("gigs", gigs)
                .toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof GigabyteData otherGigabyteData)) {
            return false;
        }

        return clients.equals(otherGigabyteData.clients)
                && gigs.equals(otherGigabyteData.gigs);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(clients, gigs);
    }
}
