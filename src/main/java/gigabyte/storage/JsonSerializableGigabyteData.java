package gigabyte.storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import gigabyte.commons.exceptions.IllegalValueException;
import gigabyte.model.GigabyteData;
import gigabyte.model.ReadOnlyGigabyteData;
import gigabyte.model.client.Client;

/**
 * An Immutable GigabyteData that is serializable to JSON format.
 */
class JsonSerializableGigabyteData {

    public static final String MISSING_FIELD_MESSAGE =
            "Saved data must contain clients, gigs, and paymentObligations arrays.";
    public static final String MESSAGE_NULL_CLIENT = "Clients list contains a null client.";
    public static final String MESSAGE_DUPLICATE_CLIENT = "Clients list contains duplicate client(s).";
    public static final String MESSAGE_DUPLICATE_GIG = "Gigs list contains duplicate gig uid(s).";

    @JsonProperty("clients")
    private final List<JsonAdaptedClient> clients;

    @JsonProperty("gigs")
    private final List<JsonAdaptedGig> gigs;

    @JsonProperty("paymentObligations")
    private final List<JsonAdaptedPaymentObligation> paymentObligations;

    /**
     * Constructs data from the current JSON format. All three collections are required.
     */
    @JsonCreator
    public JsonSerializableGigabyteData(@JsonProperty("clients") List<JsonAdaptedClient> clients,
            @JsonProperty("gigs") List<JsonAdaptedGig> gigs,
            @JsonProperty("paymentObligations") List<JsonAdaptedPaymentObligation> paymentObligations) {
        this.clients = clients == null ? null : new ArrayList<>(clients);
        this.gigs = gigs == null ? null : new ArrayList<>(gigs);
        this.paymentObligations = paymentObligations == null ? null : new ArrayList<>(paymentObligations);
    }

    /**
     * Converts a given {@code ReadOnlyGigabyteData} into this class for Jackson use.
     *
     * @param source future changes to this will not affect the created {@code JsonSerializableGigabyteData}.
     */
    public JsonSerializableGigabyteData(ReadOnlyGigabyteData source) {
        clients = source.getClientList().stream().map(JsonAdaptedClient::new).collect(Collectors.toList());
        gigs = source.getGigList().stream().map(JsonAdaptedGig::new).collect(Collectors.toList());
        paymentObligations = source.getPaymentObligationList().stream()
                .map(obligation -> new JsonAdaptedPaymentObligation(obligation, source.getGigList())).toList();
    }

    /**
     * Converts this serialized data into the model's {@code GigabyteData} object.
     *
     * @throws IllegalValueException if there were any data constraints violated.
     */
    public GigabyteData toModelType() throws IllegalValueException {
        if (clients == null || gigs == null || paymentObligations == null) {
            throw new IllegalValueException(MISSING_FIELD_MESSAGE);
        }
        GigabyteData gigabyteData = new GigabyteData();
        for (JsonAdaptedClient jsonAdaptedClient : clients) {
            if (jsonAdaptedClient == null) {
                throw new IllegalValueException(MESSAGE_NULL_CLIENT);
            }
            Client client = jsonAdaptedClient.toModelType();
            if (gigabyteData.hasClient(client) || gigabyteData.getClientList().stream()
                    .anyMatch(existing -> existing.hasSameUid(client))) {
                throw new IllegalValueException(MESSAGE_DUPLICATE_CLIENT);
            }
            gigabyteData.addClient(client);
        }
        Set<UUID> gigUids = new HashSet<>();
        for (JsonAdaptedGig gig : gigs) {
            if (gig == null) {
                throw new IllegalValueException(JsonAdaptedGig.MISSING_FIELD_MESSAGE);
            }
            var modelGig = gig.toModelType(gigabyteData.getClientList());
            if (!gigUids.add(modelGig.getUid())) {
                throw new IllegalValueException(MESSAGE_DUPLICATE_GIG);
            }
            gigabyteData.addGig(modelGig);
        }
        for (JsonAdaptedPaymentObligation obligation : paymentObligations) {
            if (obligation == null) {
                throw new IllegalValueException(JsonAdaptedPaymentObligation.MISSING_FIELD_MESSAGE);
            }
            gigabyteData.addPaymentObligation(obligation.toModelType(gigabyteData.getGigList()));
        }
        return gigabyteData;
    }

}
