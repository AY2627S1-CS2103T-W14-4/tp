package gigabyte.storage;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
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
            "A gig must have a uid, clientUid, status, deadline, and feeCents.";
    public static final String INVALID_UID_MESSAGE = "Gig uid must be a valid UUID.";

    private final String uid;
    private final String clientUid;
    @JsonProperty(value = "clientName", access = JsonProperty.Access.WRITE_ONLY)
    private final String clientName;
    @JsonIgnore
    private JsonNode titleNode;
    @JsonIgnore
    private boolean hasTitle;
    private final String status;
    private final String deadline;
    private final BigInteger feeCents;
    @JsonProperty(value = "fee", access = JsonProperty.Access.WRITE_ONLY)
    private final String legacyFee;

    /** Creates an adapted gig from either the new ID-based format or the legacy name-based format. */
    @JsonCreator
    public JsonAdaptedGig(@JsonProperty("uid") String uid, @JsonProperty("clientUid") String clientUid,
            @JsonProperty("clientName") String clientName, @JsonProperty("status") String status,
            @JsonProperty("deadline") String deadline,
            @JsonProperty("feeCents") BigInteger feeCents, @JsonProperty("fee") String legacyFee) {
        this.uid = uid;
        this.clientUid = clientUid;
        this.clientName = clientName;
        this.status = status;
        this.deadline = deadline;
        this.feeCents = feeCents;
        this.legacyFee = legacyFee;
    }

    /** Retains the previous adapter constructor for legacy data and tests. */
    public JsonAdaptedGig(String clientName, String status, String deadline, String fee) {
        this(null, null, clientName, status, deadline, null, fee);
    }

    /** Creates a name-linked adapted gig with an explicit title. */
    public JsonAdaptedGig(String clientName, String title, String status, String deadline, String fee) {
        this(null, null, clientName, status, deadline, null, fee);
        setTitle(title == null ? NullNode.getInstance() : TextNode.valueOf(title));
    }

    /** Copies {@code gig} for serialization. */
    public JsonAdaptedGig(Gig gig) {
        this(gig.getUid().toString(), gig.getClient().getUid().toString(), null, gig.getStatus().name(),
                gig.getDeadline().toString(), gig.getAgreedFee().getCents(), null);
        setTitle(TextNode.valueOf(gig.getTitle().toString()));
    }

    @JsonProperty("title")
    private JsonNode getTitle() {
        return titleNode;
    }

    @JsonSetter("title")
    private void setTitle(JsonNode title) {
        titleNode = title;
        hasTitle = true;
    }

    /** Restores a gig linked to an existing canonical client. */
    public Gig toModelType(List<Client> clients) throws IllegalValueException {
        if ((clientUid == null && clientName == null) || status == null || deadline == null
                || (feeCents == null && legacyFee == null) || (clientUid != null && uid == null)) {
            throw new IllegalValueException(MISSING_FIELD_MESSAGE);
        }

        UUID modelUid;
        try {
            modelUid = uid == null ? UUID.randomUUID() : UUID.fromString(uid);
        } catch (IllegalArgumentException exception) {
            throw new IllegalValueException(INVALID_UID_MESSAGE);
        }

        Client client;
        if (clientUid != null) {
            UUID modelClientUid;
            try {
                modelClientUid = UUID.fromString(clientUid);
            } catch (IllegalArgumentException exception) {
                throw new IllegalValueException(GigClientNotFoundException.MESSAGE);
            }
            client = clients.stream()
                    .filter(candidate -> candidate.getUid().equals(modelClientUid))
                    .findFirst()
                    .orElseThrow(() -> new IllegalValueException(GigClientNotFoundException.MESSAGE));
        } else {
            client = clients.stream()
                    .filter(candidate -> candidate.getName().fullName.equals(clientName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalValueException(GigClientNotFoundException.MESSAGE));
        }

        GigStatus gigStatus;
        try {
            gigStatus = GigStatus.parse(status);
        } catch (IllegalArgumentException exception) {
            throw new IllegalValueException(GigStatus.MESSAGE_CONSTRAINTS);
        }
        if (!Deadline.isValidDeadline(deadline)) {
            throw new IllegalValueException(Deadline.MESSAGE_CONSTRAINTS);
        }

        GigTitle gigTitle;
        if (!hasTitle) {
            gigTitle = GigTitle.UNTITLED;
        } else {
            if (titleNode == null || !titleNode.isTextual() || !GigTitle.isValidTitle(titleNode.textValue())) {
                throw new IllegalValueException(GigTitle.MESSAGE_CONSTRAINTS);
            }
            gigTitle = new GigTitle(titleNode.textValue());
        }

        Fee fee;
        if (feeCents != null) {
            if (feeCents.signum() <= 0) {
                throw new IllegalValueException(Fee.MESSAGE_CONSTRAINTS);
            }
            fee = Fee.fromCents(feeCents);
        } else {
            if (!Fee.isValidFee(legacyFee)) {
                throw new IllegalValueException(Fee.MESSAGE_CONSTRAINTS);
            }
            fee = new Fee(legacyFee);
        }
        return new Gig(modelUid, client, gigTitle, gigStatus, new Deadline(deadline), fee);
    }
}
