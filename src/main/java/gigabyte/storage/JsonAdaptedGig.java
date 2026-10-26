package gigabyte.storage;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import gigabyte.commons.exceptions.IllegalValueException;
import gigabyte.model.client.Client;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;
import gigabyte.model.gig.exceptions.GigClientNotFoundException;

/** Stores a gig's fields and stable references in JSON. */
class JsonAdaptedGig {
    public static final String MISSING_FIELD_MESSAGE =
            "A gig must have a uid, clientUid, title, status, deadline, and feeCents.";
    public static final String INVALID_UID_MESSAGE = "Gig uid must be a valid UUID.";

    private final String uid;
    private final String clientUid;
    private final JsonNode title;
    private final String status;
    private final String deadline;
    private final BigInteger feeCents;

    /** Creates an adapted gig from the current format with stable IDs and an explicit title. */
    @JsonCreator
    public JsonAdaptedGig(@JsonProperty("uid") String uid, @JsonProperty("clientUid") String clientUid,
            @JsonProperty("title") JsonNode title, @JsonProperty("status") String status,
            @JsonProperty("deadline") String deadline, @JsonProperty("feeCents") BigInteger feeCents) {
        this.uid = uid;
        this.clientUid = clientUid;
        this.title = title;
        this.status = status;
        this.deadline = deadline;
        this.feeCents = feeCents;
    }

    /** Copies {@code gig} for serialization. */
    public JsonAdaptedGig(Gig gig) {
        this(gig.getUid().toString(), gig.getClient().getUid().toString(),
                TextNode.valueOf(gig.getTitle().toString()), gig.getStatus().name(),
                gig.getDeadline().toString(), gig.getAgreedFee().getCents());
    }

    /** Restores a gig linked to an existing canonical client. */
    public Gig toModelType(List<Client> clients) throws IllegalValueException {
        if (uid == null || clientUid == null || title == null || status == null || deadline == null
                || feeCents == null) {
            throw new IllegalValueException(MISSING_FIELD_MESSAGE);
        }

        UUID modelUid;
        try {
            modelUid = UUID.fromString(uid);
        } catch (IllegalArgumentException exception) {
            throw new IllegalValueException(INVALID_UID_MESSAGE);
        }

        UUID modelClientUid;
        try {
            modelClientUid = UUID.fromString(clientUid);
        } catch (IllegalArgumentException exception) {
            throw new IllegalValueException(GigClientNotFoundException.MESSAGE);
        }
        Client client = clients.stream()
                .filter(candidate -> candidate.getUid().equals(modelClientUid))
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

        if (!title.isTextual() || !GigTitle.isValidTitle(title.textValue())) {
            throw new IllegalValueException(GigTitle.MESSAGE_CONSTRAINTS);
        }
        GigTitle gigTitle = new GigTitle(title.textValue());

        if (feeCents.signum() <= 0) {
            throw new IllegalValueException(Fee.MESSAGE_CONSTRAINTS);
        }
        Fee fee = Fee.fromCents(feeCents);
        return new Gig(modelUid, client, gigTitle, gigStatus, new Deadline(deadline), fee);
    }
}
