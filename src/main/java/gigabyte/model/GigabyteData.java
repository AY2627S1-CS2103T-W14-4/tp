package gigabyte.model;

import static gigabyte.commons.util.CollectionUtil.requireAllNonNull;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import gigabyte.commons.util.ToStringBuilder;
import gigabyte.model.client.Client;
import gigabyte.model.client.UniqueClientList;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.PaymentObligation;
import gigabyte.model.gig.exceptions.ClientHasGigsException;
import gigabyte.model.gig.exceptions.GigClientNotFoundException;
import gigabyte.model.gig.exceptions.GigNotFoundException;
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
    private final ObservableList<PaymentObligation> paymentObligations = FXCollections.observableArrayList();
    private final ObservableList<PaymentObligation> unmodifiablePaymentObligations =
            FXCollections.unmodifiableObservableList(paymentObligations);

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
        List<Gig> replacementGigs = linkGigsToClients(replacementClients, gigs);
        List<PaymentObligation> replacementObligations = linkPaymentObligations(gigs, replacementGigs,
                paymentObligations);

        this.clients.setClients(replacementClients);
        this.gigs.setAll(replacementGigs);
        paymentObligations.setAll(replacementObligations);
    }

    /**
     * Replaces the contents of the gig list.
     *
     * @throws GigClientNotFoundException if a gig refers to a client that does
     *         not exist
     */
    public void setGigs(List<Gig> gigs) {
        requireAllNonNull(gigs);
        requireUniqueGigUids(gigs);
        List<Gig> replacementGigs = linkGigsToClients(getClientList(), gigs);
        List<PaymentObligation> replacementObligations = linkPaymentObligations(this.gigs, replacementGigs,
                paymentObligations);

        this.gigs.setAll(replacementGigs);
        paymentObligations.setAll(replacementObligations);
    }

    /**
     * Resets this data using {@code newData}.
     */
    public void resetData(ReadOnlyGigabyteData newData) {
        requireNonNull(newData);

        List<Client> replacementClients = List.copyOf(newData.getClientList());
        requireUniqueGigUids(newData.getGigList());
        List<Gig> replacementGigs =
                linkGigsToClients(replacementClients, newData.getGigList());

        clients.setClients(replacementClients);
        gigs.setAll(replacementGigs);
        paymentObligations.setAll(linkPaymentObligations(newData.getGigList(), replacementGigs,
                newData.getPaymentObligationList()));
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

        List<Gig> replacementGigs = new ArrayList<>(gigs);
        for (int index = 0; index < gigs.size(); index++) {
            Gig gig = gigs.get(index);
            if (gig.getClient().hasSameUid(target)) {
                replacementGigs.set(index, gig.withClient(editedClient));
            }
        }
        List<PaymentObligation> replacementObligations = linkPaymentObligations(gigs, replacementGigs,
                paymentObligations);

        clients.setClient(target, editedClient);
        gigs.setAll(replacementGigs);
        paymentObligations.setAll(replacementObligations);
    }

    /**
     * Removes {@code key}.
     *
     * @throws ClientHasGigsException if a gig still refers to the client
     */
    public void removeClient(Client key) {
        requireNonNull(key);

        boolean hasAssociatedGig = gigs.stream()
                .anyMatch(gig -> gig.getClient().hasSameUid(key));

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
        Gig storedGig = gig.withClient(storedClient);
        boolean duplicateUid = false;
        for (Gig existing : gigs) {
            if (existing.hasSameUid(storedGig)) {
                duplicateUid = true;
                break;
            }
        }
        if (duplicateUid) {
            storedGig = storedGig.withNewUid();
        }
        gigs.add(storedGig);
    }

    /** Adds a payment obligation linked to an existing gig. */
    public void addPaymentObligation(PaymentObligation obligation) {
        requireNonNull(obligation);
        int index = findGigIndex(gigs, obligation.getGig());
        if (index < 0) {
            throw new GigNotFoundException();
        }
        paymentObligations.add(obligation.withGig(gigs.get(index)));
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

    @Override
    public ObservableList<PaymentObligation> getPaymentObligationList() {
        return unmodifiablePaymentObligations;
    }

    private static List<PaymentObligation> linkPaymentObligations(
            List<Gig> oldGigs, List<Gig> newGigs, List<PaymentObligation> obligations) {
        List<Integer> replacementIndexes = matchReplacementGigIndexes(oldGigs, newGigs);
        return obligations.stream().map(obligation -> {
            int index = findGigIndex(oldGigs, obligation.getGig());
            if (index < 0 || replacementIndexes.get(index) < 0) {
                throw new GigNotFoundException();
            }
            return obligation.withGig(newGigs.get(replacementIndexes.get(index)));
        }).toList();
    }

    /**
     * Preserves the exact gig when several gigs have identical fields, falling back to value equality for copies.
     */
    private static int findGigIndex(List<Gig> gigs, Gig target) {
        for (int index = 0; index < gigs.size(); index++) {
            if (gigs.get(index) == target) {
                return index;
            }
        }
        for (int index = 0; index < gigs.size(); index++) {
            if (gigs.get(index).hasSameUid(target)) {
                return index;
            }
        }
        return gigs.indexOf(target);
    }

    /**
     * Matches equal gigs first so reordering does not change obligation ownership. If every remaining old gig has
     * one remaining new gig, those unmatched entries are treated as edited replacements in relative order.
     */
    private static List<Integer> matchReplacementGigIndexes(List<Gig> oldGigs, List<Gig> newGigs) {
        List<Integer> replacementIndexes = new ArrayList<>();
        boolean[] matchedNewGigs = new boolean[newGigs.size()];
        for (int index = 0; index < oldGigs.size(); index++) {
            replacementIndexes.add(-1);
        }

        for (int oldIndex = 0; oldIndex < oldGigs.size(); oldIndex++) {
            for (int newIndex = 0; newIndex < newGigs.size(); newIndex++) {
                if (!matchedNewGigs[newIndex] && oldGigs.get(oldIndex).hasSameUid(newGigs.get(newIndex))) {
                    replacementIndexes.set(oldIndex, newIndex);
                    matchedNewGigs[newIndex] = true;
                    break;
                }
            }
        }

        for (int oldIndex = 0; oldIndex < oldGigs.size(); oldIndex++) {
            if (replacementIndexes.get(oldIndex) >= 0) {
                continue;
            }
            for (int newIndex = 0; newIndex < newGigs.size(); newIndex++) {
                if (!matchedNewGigs[newIndex] && oldGigs.get(oldIndex).equals(newGigs.get(newIndex))) {
                    replacementIndexes.set(oldIndex, newIndex);
                    matchedNewGigs[newIndex] = true;
                    break;
                }
            }
        }

        List<Integer> unmatchedOldIndexes = new ArrayList<>();
        List<Integer> unmatchedNewIndexes = new ArrayList<>();
        for (int oldIndex = 0; oldIndex < oldGigs.size(); oldIndex++) {
            if (replacementIndexes.get(oldIndex) < 0) {
                unmatchedOldIndexes.add(oldIndex);
            }
        }
        for (int newIndex = 0; newIndex < newGigs.size(); newIndex++) {
            if (!matchedNewGigs[newIndex]) {
                unmatchedNewIndexes.add(newIndex);
            }
        }

        if (unmatchedOldIndexes.size() == unmatchedNewIndexes.size()) {
            for (int index = 0; index < unmatchedOldIndexes.size(); index++) {
                replacementIndexes.set(unmatchedOldIndexes.get(index), unmatchedNewIndexes.get(index));
            }
        }
        return replacementIndexes;
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

    private static void requireUniqueGigUids(List<Gig> gigs) {
        Set<UUID> uids = new HashSet<>();
        for (Gig gig : gigs) {
            if (!uids.add(gig.getUid())) {
                throw new IllegalArgumentException("Gig IDs must be unique.");
            }
        }
    }

    private static Client findMatchingClient(
            List<Client> clients, Client client) {
        requireNonNull(client);

        return clients.stream()
                .filter(storedClient -> storedClient.hasSameUid(client))
                .findFirst()
                .orElseThrow(GigClientNotFoundException::new);
    }

    //// util methods

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("clients", clients)
                .add("gigs", gigs)
                .add("paymentObligations", paymentObligations)
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

        return clients.equals(otherGigabyteData.clients) && gigs.equals(otherGigabyteData.gigs)
                && paymentObligations.equals(otherGigabyteData.paymentObligations);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(clients, gigs);
    }
}
