package gigabyte.storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

import gigabyte.commons.exceptions.IllegalValueException;
import gigabyte.model.GigabyteData;
import gigabyte.model.ReadOnlyGigabyteData;
import gigabyte.model.client.Client;

/**
 * An Immutable GigabyteData that is serializable to JSON format.
 */
@JsonRootName(value = "addressbook")
class JsonSerializableGigabyteData {

    public static final String MESSAGE_DUPLICATE_CLIENT = "Clients list contains duplicate client(s).";
    public static final String MESSAGE_DUPLICATE_GIG = "Gigs list contains duplicate gig uid(s).";

    @JsonProperty("clients")
    private final List<JsonAdaptedClient> clients = new ArrayList<>();

    @JsonProperty("gigs")
    private final List<JsonAdaptedGig> gigs = new ArrayList<>();

    @JsonProperty("paymentObligations")
    private final List<JsonAdaptedPaymentObligation> paymentObligations = new ArrayList<>();

    /**
     * Constructs data from JSON, treating absent gig and obligation lists in older files as empty.
     */
    @JsonCreator
    public JsonSerializableGigabyteData(@JsonProperty("clients") List<JsonAdaptedClient> clients,
            @JsonProperty(value = "persons", access = JsonProperty.Access.WRITE_ONLY)
            List<JsonAdaptedClient> legacyClients,
            @JsonProperty("gigs") List<JsonAdaptedGig> gigs,
            @JsonProperty("paymentObligations") List<JsonAdaptedPaymentObligation> paymentObligations) {
        List<JsonAdaptedClient> clientsToLoad = clients == null ? legacyClients : clients;
        if (clientsToLoad != null) {
            this.clients.addAll(clientsToLoad);
        }
        if (gigs != null) {
            this.gigs.addAll(gigs);
        }
        if (paymentObligations != null) {
            this.paymentObligations.addAll(paymentObligations);
        }
    }

    /** Retains the existing constructor used by direct conversions. */
    public JsonSerializableGigabyteData(List<JsonAdaptedClient> clients, List<JsonAdaptedGig> gigs,
            List<JsonAdaptedPaymentObligation> paymentObligations) {
        this(clients, null, gigs, paymentObligations);
    }

    /**
     * Converts a given {@code ReadOnlyGigabyteData} into this class for Jackson use.
     *
     * @param source future changes to this will not affect the created {@code JsonSerializableGigabyteData}.
     */
    public JsonSerializableGigabyteData(ReadOnlyGigabyteData source) {
        clients.addAll(source.getClientList().stream().map(JsonAdaptedClient::new).collect(Collectors.toList()));
        gigs.addAll(source.getGigList().stream().map(JsonAdaptedGig::new).collect(Collectors.toList()));
        paymentObligations.addAll(source.getPaymentObligationList().stream()
                .map(obligation -> new JsonAdaptedPaymentObligation(obligation, source.getGigList())).toList());
    }

    /**
     * Converts this serialized data into the model's {@code GigabyteData} object.
     *
     * @throws IllegalValueException if there were any data constraints violated.
     */
    public GigabyteData toModelType() throws IllegalValueException {
        GigabyteData gigabyteData = new GigabyteData();
        for (JsonAdaptedClient jsonAdaptedClient : clients) {
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
