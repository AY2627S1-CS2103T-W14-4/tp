package gigabyte.storage;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import gigabyte.commons.exceptions.IllegalValueException;
import gigabyte.model.client.Client;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.exceptions.GigClientNotFoundException;

/**
 * Stores a gig's fields and its client identity in JSON.
 */
class JsonAdaptedGig {
    public static final String MISSING_FIELD_MESSAGE = "A gig must have a clientName, status, deadline, and fee.";

    private final String clientName;
    private final String status;
    private final String deadline;
    private final String fee;

    /**
     * Creates a JSON-friendly gig with the given fields.
     */
    @JsonCreator
    public JsonAdaptedGig(@JsonProperty("clientName") String clientName, @JsonProperty("status") String status,
            @JsonProperty("deadline") String deadline, @JsonProperty("fee") String fee) {
        this.clientName = clientName;
        this.status = status;
        this.deadline = deadline;
        this.fee = fee;
    }

    /**
     * Copies {@code gig} for serialization.
     */
    public JsonAdaptedGig(Gig gig) {
        this(gig.getClient().getName().fullName, gig.getStatus().name(), gig.getDeadline().toString(),
                gig.getAgreedFee().toString());
    }

    /**
     * Restores a gig linked to an existing canonical client.
     *
     * @throws IllegalValueException if fields are missing, invalid, or the client does not exist.
     */
    public Gig toModelType(List<Client> clients) throws IllegalValueException {
        if (clientName == null || status == null || deadline == null || fee == null) {
            throw new IllegalValueException(MISSING_FIELD_MESSAGE);
        }
        Client client = clients.stream()
                .filter(candidate -> candidate.getName().fullName.equals(clientName))
                .findFirst()
                .orElseThrow(() -> new IllegalValueException(GigClientNotFoundException.MESSAGE));

        GigStatus gigStatus;
        try {
            gigStatus = GigStatus.parse(status);
        } catch (IllegalArgumentException exception) {
            throw new IllegalValueException(GigStatus.MESSAGE_CONSTRAINTS);
        }
        if (!Deadline.isValidDeadline(deadline)) {
            throw new IllegalValueException(Deadline.MESSAGE_CONSTRAINTS);
        }
        if (!Fee.isValidFee(fee)) {
            throw new IllegalValueException(Fee.MESSAGE_CONSTRAINTS);
        }
        return new Gig(client, gigStatus, new Deadline(deadline), new Fee(fee));
    }
}
