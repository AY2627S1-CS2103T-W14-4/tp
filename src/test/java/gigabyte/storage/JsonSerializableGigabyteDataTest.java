package gigabyte.storage;

import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalClients.ALICE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import gigabyte.commons.exceptions.IllegalValueException;
import gigabyte.commons.util.JsonUtil;
import gigabyte.model.GigabyteData;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;
import gigabyte.model.gig.PaymentObligation;
import gigabyte.testutil.TypicalClients;

public class JsonSerializableGigabyteDataTest {

    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonSerializableGigabyteDataTest");
    private static final Path TYPICAL_CLIENTS_FILE = TEST_DATA_FOLDER.resolve("typicalClientsGigabyteData.json");
    private static final Path INVALID_CLIENT_FILE = TEST_DATA_FOLDER.resolve("invalidClientGigabyteData.json");
    private static final Path DUPLICATE_CLIENT_FILE = TEST_DATA_FOLDER.resolve("duplicateClientGigabyteData.json");

    @Test
    public void toModelType_typicalClientsFile_success() throws Exception {
        JsonSerializableGigabyteData dataFromFile = JsonUtil.readJsonFileStrict(TYPICAL_CLIENTS_FILE,
                JsonSerializableGigabyteData.class).get();
        GigabyteData gigabyteDataFromFile = dataFromFile.toModelType();
        GigabyteData typicalClientsGigabyteData = TypicalClients.getTypicalGigabyteData();
        assertEquals(gigabyteDataFromFile, typicalClientsGigabyteData);
    }

    @Test
    public void toModelType_invalidClientFile_throwsIllegalValueException() throws Exception {
        JsonSerializableGigabyteData dataFromFile = JsonUtil.readJsonFileStrict(INVALID_CLIENT_FILE,
                JsonSerializableGigabyteData.class).get();
        assertThrows(IllegalValueException.class, dataFromFile::toModelType);
    }

    @Test
    public void toModelType_duplicateClients_throwsIllegalValueException() throws Exception {
        JsonSerializableGigabyteData dataFromFile = JsonUtil.readJsonFileStrict(DUPLICATE_CLIENT_FILE,
                JsonSerializableGigabyteData.class).get();
        assertThrows(IllegalValueException.class, JsonSerializableGigabyteData.MESSAGE_DUPLICATE_CLIENT,
                dataFromFile::toModelType);
    }

    @Test
    public void toModelType_missingCollections_throwsIllegalValueException() {
        for (JsonSerializableGigabyteData data : List.of(
                new JsonSerializableGigabyteData(null, List.of(), List.of()),
                new JsonSerializableGigabyteData(List.of(), null, List.of()),
                new JsonSerializableGigabyteData(List.of(), List.of(), null))) {
            assertThrows(IllegalValueException.class, JsonSerializableGigabyteData.MISSING_FIELD_MESSAGE,
                    data::toModelType);
        }
    }

    @Test
    public void toModelType_nullClient_throwsIllegalValueException() {
        JsonSerializableGigabyteData data = new JsonSerializableGigabyteData(Arrays.asList((JsonAdaptedClient) null),
                List.of(), List.of());
        assertThrows(IllegalValueException.class, JsonSerializableGigabyteData.MESSAGE_NULL_CLIENT,
                data::toModelType);
    }

    @Test
    public void toModelType_nullGig_throwsIllegalValueException() {
        JsonSerializableGigabyteData data = new JsonSerializableGigabyteData(List.of(new JsonAdaptedClient(ALICE)),
                Arrays.asList((JsonAdaptedGig) null), List.of());
        assertThrows(IllegalValueException.class, JsonAdaptedGig.MISSING_FIELD_MESSAGE, data::toModelType);
    }

    @Test
    public void toModelType_nullPaymentObligation_throwsIllegalValueException() {
        JsonSerializableGigabyteData data = new JsonSerializableGigabyteData(List.of(new JsonAdaptedClient(ALICE)),
                List.of(), Arrays.asList((JsonAdaptedPaymentObligation) null));
        assertThrows(IllegalValueException.class, JsonAdaptedPaymentObligation.MISSING_FIELD_MESSAGE,
                data::toModelType);
    }

    @Test
    public void jsonRoundTrip_paidAndUnpaidPayments_preservesPaymentStates() throws Exception {
        GigabyteData source = new GigabyteData();
        source.addClient(ALICE);
        Gig gig = new Gig(ALICE, new GigTitle("Website redesign"), GigStatus.IN_PROGRESS,
                new Deadline("2027-12-31"), new Fee("1250"));
        source.addGig(gig);
        Gig storedGig = source.getGigList().get(0);
        source.addPaymentObligation(new PaymentObligation(storedGig, new Fee("100"),
                new Deadline("2027-01-31"), false));
        source.addPaymentObligation(new PaymentObligation(storedGig, new Fee("200"),
                new Deadline("2027-02-28"), true));

        String json = JsonUtil.toJsonString(new JsonSerializableGigabyteData(source));
        GigabyteData restored = JsonUtil.fromJsonString(json, JsonSerializableGigabyteData.class).toModelType();

        assertEquals(source, restored);
        assertEquals(List.of(false, true), restored.getPaymentObligationList().stream()
                .map(PaymentObligation::isPaid).toList());
    }

}
