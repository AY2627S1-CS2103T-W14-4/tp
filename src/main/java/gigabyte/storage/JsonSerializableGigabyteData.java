package gigabyte.storage;

import java.util.ArrayList;
import java.util.List;
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

    @JsonProperty("persons")
    private final List<JsonAdaptedClient> clients = new ArrayList<>();

    @JsonProperty("gigs")
    private final List<JsonAdaptedGig> gigs = new ArrayList<>();

    /**
     * Constructs data from JSON, treating an absent gig list in older files as empty.
     */
    @JsonCreator
    public JsonSerializableGigabyteData(@JsonProperty("persons") List<JsonAdaptedClient> clients,
            @JsonProperty("gigs") List<JsonAdaptedGig> gigs) {
        this.clients.addAll(clients);
        if (gigs != null) {
            this.gigs.addAll(gigs);
        }
    }

    /**
     * Converts a given {@code ReadOnlyGigabyteData} into this class for Jackson use.
     *
     * @param source future changes to this will not affect the created {@code JsonSerializableGigabyteData}.
     */
    public JsonSerializableGigabyteData(ReadOnlyGigabyteData source) {
        clients.addAll(source.getClientList().stream().map(JsonAdaptedClient::new).collect(Collectors.toList()));
        gigs.addAll(source.getGigList().stream().map(JsonAdaptedGig::new).collect(Collectors.toList()));
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
            if (gigabyteData.hasClient(client)) {
                throw new IllegalValueException(MESSAGE_DUPLICATE_CLIENT);
            }
            gigabyteData.addClient(client);
        }
        for (JsonAdaptedGig gig : gigs) {
            if (gig == null) {
                throw new IllegalValueException(JsonAdaptedGig.MISSING_FIELD_MESSAGE);
            }
            gigabyteData.addGig(gig.toModelType(gigabyteData.getClientList()));
        }
        return gigabyteData;
    }

}
