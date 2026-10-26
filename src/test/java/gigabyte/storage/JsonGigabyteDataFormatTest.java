package gigabyte.storage;

import static gigabyte.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gigabyte.commons.exceptions.DataLoadingException;
import gigabyte.commons.util.JsonUtil;
import gigabyte.model.GigabyteData;
import gigabyte.model.ReadOnlyGigabyteData;

/** Verifies the save-file format at the storage boundary. */
public class JsonGigabyteDataFormatTest {
    private static final String CURRENT_JSON = """
            {
              "clients": [{
                "uid": "00000000-0000-0000-0000-000000000001",
                "name": "Alice Pauline", "phone": "94351253", "email": "alice@example.com",
                "address": "123 Jurong West", "tags": []
              }],
              "gigs": [{
                "uid": "00000000-0000-0000-0000-000000000002",
                "clientUid": "00000000-0000-0000-0000-000000000001",
                "title": "Website redesign", "status": "NOT_STARTED", "deadline": "2027-01-31",
                "feeCents": 125050
              }],
              "paymentObligations": [{
                "gigUid": "00000000-0000-0000-0000-000000000002",
                "amountCents": 5025, "dueDate": "2027-02-28", "paid": true
              }]
            }
            """;

    @TempDir
    Path testFolder;

    @Test
    public void readAndSave_currentFormat_preservesAllFieldsAndLinks() throws Exception {
        Path path = testFolder.resolve("current.json");
        Files.writeString(path, CURRENT_JSON);
        JsonGigabyteDataStorage storage = new JsonGigabyteDataStorage(path);
        ReadOnlyGigabyteData data = storage.readGigabyteData().orElseThrow();

        assertSame(data.getClientList().get(0), data.getGigList().get(0).getClient());
        assertSame(data.getGigList().get(0), data.getPaymentObligationList().get(0).getGig());
        storage.saveGigabyteData(data);
        assertEquals(currentJson(), JsonUtil.fromJsonString(Files.readString(path), ObjectNode.class));
    }

    @Test
    public void readAndSave_emptyCurrentFormat_success() throws Exception {
        Path path = testFolder.resolve("empty.json");
        Files.writeString(path, "{\"clients\":[],\"gigs\":[],\"paymentObligations\":[]}");
        JsonGigabyteDataStorage storage = new JsonGigabyteDataStorage(path);
        ReadOnlyGigabyteData data = storage.readGigabyteData().orElseThrow();
        assertEquals(new GigabyteData(), data);
        storage.saveGigabyteData(data);
        assertEquals(data, storage.readGigabyteData().orElseThrow());
    }

    @Test
    public void read_missingOrNullCollections_throwsDataLoadingException() throws Exception {
        // EP: one collection missing or null
        for (String field : List.of("clients", "gigs", "paymentObligations")) {
            ObjectNode json = currentJson();
            json.remove(field);
            assertInvalidSave(json);
            json.putNull(field);
            assertInvalidSave(json);
        }

        // EP: every collection missing, or the whole file is null
        assertInvalidSave(JsonUtil.fromJsonString("{}", ObjectNode.class));
        assertInvalidSave(NullNode.getInstance());
    }

    @Test
    public void read_missingOrNullRecordFields_throwsDataLoadingException() throws Exception {
        Map<String, List<String>> requiredFields = Map.of(
                "clients", List.of("uid", "name", "phone", "email", "address", "tags"),
                "gigs", List.of("uid", "clientUid", "title", "status", "deadline", "feeCents"),
                "paymentObligations", List.of("gigUid", "amountCents", "dueDate", "paid"));
        for (var entry : requiredFields.entrySet()) {
            for (String field : entry.getValue()) {
                ObjectNode json = currentJson();
                ObjectNode record = (ObjectNode) json.get(entry.getKey()).get(0);
                record.remove(field);
                assertInvalidSave(json);
                record.putNull(field);
                assertInvalidSave(json);
            }
        }
    }

    @Test
    public void read_obsoleteFieldsAlongsideCurrentFields_throwsDataLoadingException() throws Exception {
        // EP: obsolete top-level collection
        ObjectNode json = currentJson();
        json.set("persons", json.get("clients"));
        assertInvalidSave(json);

        // EP: obsolete gig fields
        for (String field : List.of("clientName", "fee")) {
            json = currentJson();
            ObjectNode gig = (ObjectNode) json.get("gigs").get(0);
            gig.put(field, "old value");
            assertInvalidSave(json);
        }

        // EP: obsolete payment fields
        for (String field : List.of("gigIndex", "amount")) {
            json = currentJson();
            ObjectNode payment = (ObjectNode) json.get("paymentObligations").get(0);
            payment.put(field, "old value");
            assertInvalidSave(json);
        }
    }

    @Test
    public void read_olderFieldNames_throwsDataLoadingException() throws Exception {
        // EP: clients saved under the old collection name
        ObjectNode json = currentJson();
        json.set("persons", json.remove("clients"));
        assertInvalidSave(json);

        // EP: gig saved with old field names
        json = currentJson();
        ObjectNode gig = (ObjectNode) json.get("gigs").get(0);
        gig.remove("clientUid");
        gig.put("clientName", "Alice Pauline");
        gig.remove("feeCents");
        gig.put("fee", "1250.50");
        assertInvalidSave(json);

        // EP: payment saved with old field names
        json = currentJson();
        ObjectNode payment = (ObjectNode) json.get("paymentObligations").get(0);
        payment.remove("gigUid");
        payment.put("gigIndex", 1);
        payment.remove("amountCents");
        payment.put("amount", "50.25");
        assertInvalidSave(json);
    }

    @Test
    public void read_fractionalCents_throwsDataLoadingException() throws Exception {
        for (var entry : Map.of("gigs", "feeCents", "paymentObligations", "amountCents").entrySet()) {
            ObjectNode json = currentJson();
            ObjectNode record = (ObjectNode) json.get(entry.getKey()).get(0);
            record.put(entry.getValue(), 50.25);
            assertInvalidSave(json);
        }
    }

    @Test
    public void read_nullRecords_throwsDataLoadingException() throws Exception {
        for (String field : List.of("clients", "gigs", "paymentObligations")) {
            ObjectNode json = currentJson();
            ArrayNode records = (ArrayNode) json.get(field);
            records.set(0, NullNode.getInstance());
            assertInvalidSave(json);
        }
    }

    private ObjectNode currentJson() throws IOException {
        return JsonUtil.fromJsonString(CURRENT_JSON, ObjectNode.class);
    }

    private void assertInvalidSave(JsonNode json) throws IOException {
        Path path = testFolder.resolve("invalid.json");
        String contents = json.toString();
        Files.writeString(path, contents);
        assertThrows(DataLoadingException.class, () -> new JsonGigabyteDataStorage(path).readGigabyteData());
        assertEquals(contents, Files.readString(path));
    }
}
