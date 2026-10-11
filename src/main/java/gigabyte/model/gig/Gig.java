package gigabyte.model.gig;

import static gigabyte.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Objects;
import java.util.UUID;

import gigabyte.commons.util.ToStringBuilder;
import gigabyte.model.client.Client;

/**
 * Represents a piece of work undertaken for a client.
 * Guarantees: immutable; all fields are present and valid.
 */
public class Gig {
    private final UUID uid;
    private final Client client;
    private final GigTitle title;
    private final GigStatus status;
    private final Deadline deadline;
    private final Fee agreedFee;

    /**
     * Constructs a {@code Gig}.
     */
    public Gig(Client client, GigTitle title, GigStatus status, Deadline deadline, Fee agreedFee) {
        this(UUID.randomUUID(), client, title, status, deadline, agreedFee);
    }

    /** Constructs a gig with a stable identifier. */
    public Gig(UUID uid, Client client, GigTitle title, GigStatus status, Deadline deadline, Fee agreedFee) {
        requireAllNonNull(uid, client, title, status, deadline, agreedFee);
        this.uid = uid;
        this.client = client;
        this.title = title;
        this.status = status;
        this.deadline = deadline;
        this.agreedFee = agreedFee;
    }

    /** Returns this gig's stable identifier. */
    public UUID getUid() {
        return uid;
    }

    /** Returns whether both gigs have the same stable identifier. */
    public boolean hasSameUid(Gig otherGig) {
        return otherGig != null && uid.equals(otherGig.uid);
    }

    public Client getClient() {
        return client;
    }

    public GigTitle getTitle() {
        return title;
    }

    public GigStatus getStatus() {
        return status;
    }

    public Deadline getDeadline() {
        return deadline;
    }

    public Fee getAgreedFee() {
        return agreedFee;
    }

    /**
     * Returns a copy of this gig linked to {@code replacementClient}.
     */
    public Gig withClient(Client replacementClient) {
        return new Gig(uid, replacementClient, title, status, deadline, agreedFee);
    }

    /** Returns a copy with a new stable identifier. */
    public Gig withNewUid() {
        return new Gig(UUID.randomUUID(), client, title, status, deadline, agreedFee);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof Gig otherGig)) {
            return false;
        }

        return client.equals(otherGig.client)
                && title.equals(otherGig.title)
                && status == otherGig.status
                && deadline.equals(otherGig.deadline)
                && agreedFee.equals(otherGig.agreedFee);
    }

    @Override
    public int hashCode() {
        return Objects.hash(client, title, status, deadline, agreedFee);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("client", client)
                .add("title", title)
                .add("status", status)
                .add("deadline", deadline)
                .add("agreedFee", agreedFee)
                .toString();
    }
}
