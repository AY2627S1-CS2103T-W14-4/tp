package gigabyte.model.gig;

import static gigabyte.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Objects;

import gigabyte.commons.util.ToStringBuilder;
import gigabyte.model.client.Client;

/**
 * Represents a piece of work undertaken for a client.
 * Guarantees: immutable; all fields are present and valid.
 */
public class Gig {
    private final Client client;
    private final GigStatus status;
    private final Deadline deadline;
    private final Fee agreedFee;

    /**
     * Constructs a {@code Gig}.
     */
    public Gig(Client client, GigStatus status, Deadline deadline, Fee agreedFee) {
        requireAllNonNull(client, status, deadline, agreedFee);
        this.client = client;
        this.status = status;
        this.deadline = deadline;
        this.agreedFee = agreedFee;
    }

    public Client getClient() {
        return client;
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
        return new Gig(replacementClient, status, deadline, agreedFee);
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
                && status == otherGig.status
                && deadline.equals(otherGig.deadline)
                && agreedFee.equals(otherGig.agreedFee);
    }

    @Override
    public int hashCode() {
        return Objects.hash(client, status, deadline, agreedFee);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("client", client)
                .add("status", status)
                .add("deadline", deadline)
                .add("agreedFee", agreedFee)
                .toString();
    }
}
